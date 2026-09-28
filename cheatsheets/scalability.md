SCALABILITY COMMANDS — CLIMBING MANAGEMENT
=========================================

## 1. Inspect current Kubernetes state

kubectl get deployments
kubectl get pods -o wide
kubectl get services
kubectl get endpointslices

# Enrollment Service only
kubectl get deployment enrollment-service
kubectl get pods | findstr enrollment-service

# MongoDB only
kubectl get pods | findstr mongo


## 2. Troubleshoot crashing Pods

# Current logs
kubectl logs <pod-name>

# Previous crashed container logs
kubectl logs <pod-name> --previous

# Follow logs
kubectl logs -f <pod-name>

# Inspect Pod state, probes and events
kubectl describe pod <pod-name>

# Cluster events ordered by time
kubectl get events --sort-by=.lastTimestamp


## 3. Enrollment image / deployment

# Current image used by the Deployment
kubectl get deployment enrollment-service -o jsonpath="{.spec.template.spec.containers[0].image}"

# Local Docker build (manual alternative to CI)
docker build `
  -t ghcr.io/rmarintech/climbing-management-sb-enrollment-service:latest `
.\services\enrollment-service

# Push image manually
docker push ghcr.io/rmarintech/climbing-management-sb-enrollment-service:latest

# Prefer CI/CD for immutable images:
# git push -> GitHub Actions -> GHCR :latest + :<commit-sha>

git rev-parse HEAD

# Update Kubernetes image manually
kubectl set image deployment/enrollment-service `
enrollment-service=ghcr.io/rmarintech/climbing-management-sb-enrollment-service:latest

# Rollout
kubectl rollout status deployment/enrollment-service
kubectl rollout restart deployment/enrollment-service


## 4. Apply declarative Kubernetes configuration

# Deployment source of truth
kubectl apply -f .\k8s\enrollment-deployment.yaml

# Mongo Deployment
kubectl apply -f .\k8s\mongo-deployment.yaml

# HPA
kubectl apply -f .\k8s\enrollment-service-hpa.yaml

# Prefer YAML / Helm over kubectl set ... for persistent configuration.
# kubectl set ... is useful for temporary testing or emergency changes.


## 5. Docker Compose Kafka from Kubernetes

Docker Compose Kafka listeners:

Windows / IntelliJ
-> localhost:8082

Docker Compose containers
-> kafka:18082

Kubernetes Pods
-> host.docker.internal:28082

Enrollment Kubernetes override:

SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:28082

Example:

kubectl set env deployment/enrollment-service `
SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:28082

Important:
"kafka" is a Docker Compose DNS name.
It does NOT automatically exist inside Kubernetes.


## 6. Keycloak / JWT from Kubernetes

JWT issuer remains:

http://localhost:8083/realms/climbing

But the Kubernetes Pod reaches Keycloak signing keys through:

http://host.docker.internal:8083/realms/climbing/protocol/openid-connect/certs

Kubernetes override:

kubectl set env deployment/enrollment-service `
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://host.docker.internal:8083/realms/climbing/protocol/openid-connect/certs

Mental model:

issuer-uri
-> what the JWT is expected to say

jwk-set-uri
-> where the service actually downloads signing keys


## 7. MongoDB connectivity

# Service
kubectl get service mongo

# Backend endpoint
kubectl get endpointslices -l kubernetes.io/service-name=mongo -o wide

# Direct test from another Kubernetes Pod
kubectl run mongo-test `
  --rm -it `
--restart=Never `
  --image=mongo:7 `
-- mongosh "mongodb://mongo:27017" --eval "db.adminCommand('ping')"

Expected:

{ ok: 1 }


## 8. MongoDB probes

Problem discovered:

readinessProbe / livenessProbe
-> mongosh
-> default timeoutSeconds = 1
-> occasional timeout
-> liveness fails
-> Kubernetes restarts Mongo
-> Enrollment loses DB connection
-> HTTP 500

Recommended approach:

startupProbe
-> real mongosh ping
-> gives Mongo time to start

readinessProbe
-> real mongosh ping
-> determines whether Mongo can receive traffic

livenessProbe
-> TCP port 27017
-> lightweight process-alive check

Useful check:

kubectl describe pod <mongo-pod-name>

Watch restart count:

kubectl get pods | findstr mongo


## 9. Manual horizontal scaling

Scale Enrollment Service:

kubectl scale deployment enrollment-service --replicas=3

Watch rollout:

kubectl rollout status deployment/enrollment-service
kubectl get pods -w

Check only Enrollment Pods:

kubectl get pods | findstr enrollment-service

Check Service backends:

kubectl get endpointslices `
  -l kubernetes.io/service-name=enrollment-service `
-o wide

Architecture:

Client
|
v
Kubernetes Service
|
+--> Enrollment Pod 1
+--> Enrollment Pod 2
+--> Enrollment Pod 3

Deployment / ReplicaSet
-> maintains desired replica count

Service
-> routes traffic only to Ready Pods


## 10. Failover / self-healing test

Delete one Enrollment Pod:

kubectl delete pod <enrollment-pod-name>

Watch Kubernetes recreate it:

kubectl get pods -w

Expected:

3 Pods
-> delete 1
-> 2 remain available
-> ReplicaSet creates replacement
-> back to 3


## 11. Temporary client inside Kubernetes

Create a curl Pod with a fresh JWT:

kubectl run enrollment-client `
  --rm -it `
--restart=Never `
  --image=curlimages/curl `
--env="TOKEN=$env:TOKEN" `
-- sh

Inside the Pod:

curl -i \
-H "Authorization: Bearer $TOKEN" \
http://enrollment-service:9091/api/v2/enrollments

Expected:

HTTP/1.1 200

Continuous traffic:

while true; do
curl -s \
-o /dev/null \
-w "%{http_code}\n" \
-H "Authorization: Bearer $TOKEN" \
http://enrollment-service:9091/api/v2/enrollments
sleep 0.3
done


## 12. Resource requests and limits

Enrollment Deployment:

resources:
requests:
cpu: 100m
memory: 384Mi
limits:
cpu: 500m
memory: 512Mi

Meaning:

requests
-> scheduler baseline
-> HPA CPU utilization denominator

limits
-> maximum resources the container may consume

HPA utilization:

actual CPU / requested CPU * 100

Example:

actual = 70m
request = 100m
=> 70%


## 13. Metrics Server

Check Pod CPU / memory:

kubectl top pods

Enrollment only:

kubectl top pods | findstr enrollment-service


## 14. Horizontal Pod Autoscaler

HPA configuration:

minReplicas: 2
maxReplicas: 5
CPU target: 70%

Apply:

kubectl apply -f .\k8s\enrollment-service-hpa.yaml

Inspect:

kubectl get hpa
kubectl describe hpa enrollment-service

Watch live:

kubectl get hpa enrollment-service -w

Expected idle example:

cpu: 7%/70%
minPods: 2
maxPods: 5
replicas: 2


## 15. HPA load test

Create load Pod:

kubectl run enrollment-load `
  --rm -it `
--restart=Never `
  --image=curlimages/curl `
--env="TOKEN=$env:TOKEN" `
-- sh

Inside the Pod, create concurrent loops:

for i in $(seq 1 50); do
while true; do
curl -s \
-o /dev/null \
-H "Authorization: Bearer $TOKEN" \
http://enrollment-service:9091/api/v2/enrollments
done &
done

wait

Watch HPA:

kubectl get hpa enrollment-service -w

Watch Pods:

kubectl get pods -w

Expected:

2 Pods
-> CPU rises above target
-> HPA increases desired replicas
-> 3 / 4 / 5 Pods

After load stops:

CPU falls
-> HPA waits for stabilization
-> scales back toward minReplicas=2


## 16. Scalability mental model

Vertical scaling
-> bigger instance
-> more CPU / RAM

Horizontal scaling
-> more instances
-> 1 -> 2 -> 3 -> 5 Pods

Stateless service
-> Pod is disposable
-> durable state lives outside the Pod

Application scaling does NOT mean infinite system scaling:

Enrollment Pods
|
+----> shared MongoDB

A shared dependency can become the next bottleneck.


## 17. Kafka consumer scaling reminder

HTTP scaling and Kafka consumer scaling are different.

Example:

3 Enrollment Pods
2 Kafka partitions

HTTP:
-> all 3 Pods can serve requests

Kafka consumer group:
-> only 2 consumers can actively own partitions
-> third consumer stays idle / standby

Kafka consumer parallelism is bounded by partition count.
