# Scalability

[Back to README](../README.md) · [Project Roadmap](ROADMAP.md)

This document contains the theory, commands, experiments and conclusions from the **Scalability** phase of the Enrollment Service. Progress checkboxes remain only in `ROADMAP.md`.

---

# 1. Scalability mental model

Performance asked:

```text
How fast is one service instance?
```

Scalability asks:

```text
How does the architecture handle more load by adding capacity?
```

```text
Vertical scaling
→ make one instance bigger
→ more CPU / memory

Horizontal scaling
→ add more instances
→ 1 → 2 → 3 → 5 Pods
```

Kubernetes makes horizontal scaling practical because Pods are replaceable and a Service provides a stable network identity in front of them.

---

# 2. Stateless services

Horizontal scaling is simplest when application instances do not own durable business state.

```text
Enrollment Pod
→ disposable compute

MongoDB
→ durable Enrollment state
```

Losing one Enrollment Pod should not lose enrollments. Another replica can serve the next request because persistent state lives outside the JVM.

---

# 3. Manual horizontal scaling

```powershell
kubectl scale deployment enrollment-service --replicas=3
kubectl rollout status deployment/enrollment-service
kubectl get pods | findstr enrollment-service
```

Architecture:

```text
                 enrollment-service
                 Kubernetes Service
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
     Enrollment    Enrollment    Enrollment
       Pod 1         Pod 2         Pod 3
```

Inspect Service backends:

```powershell
kubectl get endpointslices `
  -l kubernetes.io/service-name=enrollment-service `
  -o wide
```

---

# 4. Service routing vs self-healing

```text
Kubernetes Service
→ routes traffic to Ready endpoints

Deployment / ReplicaSet
→ maintains desired replica count
```

Failover experiment:

```powershell
kubectl delete pod <enrollment-pod-name>
kubectl get pods -w
```

Expected:

```text
3 Pods
  ↓
delete 1
  ↓
2 Ready Pods keep serving
  ↓
ReplicaSet creates replacement
  ↓
back to 3
```

---

# 5. In-cluster HTTP client

Create a temporary client with a fresh JWT:

```powershell
kubectl run enrollment-client `
  --rm -it `
  --restart=Never `
  --image=curlimages/curl `
  --env="TOKEN=$env:TOKEN" `
  -- sh
```

Inside:

```sh
curl -i \
  -H "Authorization: Bearer $TOKEN" \
  http://enrollment-service:9091/api/v2/enrollments
```

Continuous traffic:

```sh
while true; do
  curl -s \
    -o /dev/null \
    -w "%{http_code}\n" \
    -H "Authorization: Bearer $TOKEN" \
    http://enrollment-service:9091/api/v2/enrollments
  sleep 0.3
done
```

A JWT copied into a Pod environment does not update when the Windows `$env:TOKEN` changes. Recreate the client Pod to inject a fresh token.

---

# 6. Docker Compose DNS vs Kubernetes DNS

A major lesson from this phase was that DNS names belong to a network scope.

```text
Windows / IntelliJ
→ localhost:8082

Docker Compose containers
→ kafka:18082

Kubernetes Pods
→ host.docker.internal:28082
```

`kafka` is a Docker Compose service-discovery name; it does not automatically exist inside Kubernetes.

For the local Kubernetes exercise, Kafka exposes a dedicated listener advertised as:

```text
K8S://host.docker.internal:28082
```

Enrollment Kubernetes config uses:

```text
SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:28082
```

---

# 7. Keycloak issuer vs JWK reachability

The token issuer remains:

```text
http://localhost:8083/realms/climbing
```

Inside Kubernetes, `localhost` means the Pod itself. The JWT issuer and the network location used to fetch signing keys therefore have different roles:

```text
issuer-uri
→ what the JWT is expected to say

jwk-set-uri
→ where the service downloads signing keys
```

Kubernetes reaches Keycloak keys through:

```text
http://host.docker.internal:8083/realms/climbing/protocol/openid-connect/certs
```

while issuer validation remains aligned with the JWT `iss` claim.

---

# 8. MongoDB connectivity

The Enrollment Service reaches Mongo through Kubernetes DNS:

```text
Enrollment Pod
     ↓
mongo Service
     ↓
EndpointSlice
     ↓
Mongo Pod :27017
```

Useful checks:

```powershell
kubectl get service mongo
kubectl get endpointslices -l kubernetes.io/service-name=mongo -o wide
kubectl get pods | findstr mongo
```

Direct test:

```powershell
kubectl run mongo-test `
  --rm -it `
  --restart=Never `
  --image=mongo:7 `
  -- mongosh "mongodb://mongo:27017" --eval "db.adminCommand('ping')"
```

Expected:

```text
{ ok: 1 }
```

---

# 9. Health probes can cause outages

Mongo had accumulated hundreds of restarts because both readiness and liveness executed `mongosh` with the default one-second probe timeout.

```text
mongosh occasionally takes >1s
        ↓
liveness fails
        ↓
Kubernetes kills Mongo
        ↓
Enrollment loses DB connection
        ↓
HTTP 500
```

The corrected strategy is:

```text
startupProbe
→ real Mongo ping with generous startup budget

readinessProbe
→ real Mongo ping
→ determines whether Mongo can receive traffic

livenessProbe
→ lightweight TCP :27017 check
```

Example liveness probe:

```yaml
livenessProbe:
  tcpSocket:
    port: 27017
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 6
```

A health probe that is too aggressive can create the outage it is supposed to detect.

---

# 10. Resource requests and limits

Enrollment Deployment:

```yaml
resources:
  requests:
    cpu: 100m
    memory: 384Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

```text
requests
→ scheduler planning baseline
→ HPA utilization denominator

limits
→ maximum container resource usage
```

CPU HPA utilization:

```text
actual CPU / requested CPU × 100
```

Example:

```text
70m / 100m = 70%
```

Resource settings belong in version-controlled YAML / Helm configuration, not only in live `kubectl set resources` mutations.

---

# 11. Metrics Server

```powershell
kubectl top pods | findstr enrollment-service
```

Metrics Server provides the resource measurements used by the HPA.

---

# 12. Horizontal Pod Autoscaler

Current learning configuration:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: enrollment-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: enrollment-service
  minReplicas: 2
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

Apply / inspect:

```powershell
kubectl apply -f .\k8s\enrollment-service-hpa.yaml
kubectl get hpa
kubectl describe hpa enrollment-service
kubectl get hpa enrollment-service -w
```

Observed idle state:

```text
cpu: 7%/70%
replicas: 2
```

---

# 13. HPA load experiment

Create load Pod:

```powershell
kubectl run enrollment-load `
  --rm -it `
  --restart=Never `
  --image=curlimages/curl `
  --env="TOKEN=$env:TOKEN" `
  -- sh
```

Inside:

```sh
for i in $(seq 1 50); do
  while true; do
    curl -s \
      -o /dev/null \
      -H "Authorization: Bearer $TOKEN" \
      http://enrollment-service:9091/api/v2/enrollments
  done &
done

wait
```

Verified control loop:

```text
HTTP load ↑
     ↓
Enrollment CPU ↑
     ↓
Metrics Server
     ↓
HPA sees utilization >70%
     ↓
desired replicas ↑
     ↓
Deployment / ReplicaSet creates Pods
     ↓
Service gains more Ready backends
```

Automatic scale-up worked successfully.

---

# 14. Scale-down stabilization

After load stopped, Kubernetes did not immediately remove all extra replicas. This is intentional.

```text
traffic ends
   ↓
CPU falls
   ↓
HPA stabilization
   ↓
avoids 2 → 5 → 2 → 5 flapping
   ↓
replicas eventually return toward minReplicas=2
```

---

# 15. Declarative configuration

Imperative commands such as these are useful for experiments and emergencies:

```text
kubectl scale
kubectl set image
kubectl set env
kubectl set resources
```

But durable project configuration belongs in:

```text
Kubernetes YAML
or
Helm values/templates
```

so Git remains the reproducible source of truth.

---

# 16. Shared dependencies become the next bottleneck

```text
                 Service
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
   Enrollment   Enrollment   Enrollment
        │           │           │
        └───────────┼───────────┘
                    ▼
                  MongoDB
```

Adding Enrollment Pods does not automatically scale MongoDB. A system can simply move from an application bottleneck to a database bottleneck.

> A system scales only as far as its least scalable shared dependency.

This is the next part of the phase.

---

# 17. Kafka consumer scaling is different from HTTP scaling

Example:

```text
Enrollment Pods = 3
Kafka partitions = 2
```

HTTP:

```text
all 3 Pods can serve requests
```

Kafka consumer group:

```text
Partition 0 → Consumer A
Partition 1 → Consumer B
Consumer C  → no partition assigned
```

Consumer-group parallelism is bounded by partition count. More service replicas do not create unlimited Kafka consumer parallelism.

---

# 18. Troubleshooting commands

```powershell
kubectl get deployments
kubectl get pods -o wide
kubectl get services
kubectl get endpointslices
kubectl top pods

kubectl logs <pod>
kubectl logs <pod> --previous
kubectl logs -f <pod>

kubectl describe pod <pod>
kubectl get events --sort-by=.lastTimestamp

kubectl rollout status deployment/enrollment-service
kubectl rollout restart deployment/enrollment-service

kubectl get deployment enrollment-service `
  -o jsonpath="{.spec.template.spec.containers[0].image}"

kubectl apply -f .\k8s\enrollment-deployment.yaml
kubectl apply -f .\k8s\mongo-deployment.yaml
kubectl apply -f .\k8s\enrollment-service-hpa.yaml
```

---

# 19. Key lessons so far

```text
1. Horizontal scaling works best with stateless application instances.
2. A Service provides stable routing while Pods remain disposable.
3. Service routing and ReplicaSet self-healing solve different problems.
4. Resource requests are required for meaningful percentage-based CPU HPA.
5. Metrics Server supplies the HPA resource signal.
6. HPA scale-up and scale-down are intentionally asymmetric.
7. Declarative YAML / Helm should remain the source of truth.
8. Docker Compose DNS names belong to the Compose network scope.
9. Kafka advertised listeners must be reachable from the client network.
10. JWT issuer validation and JWK network reachability are separate concerns.
11. Aggressive liveness probes can create outages.
12. More application Pods can move the bottleneck to a shared dependency.
13. Kafka consumer scalability is bounded by partition count.
```

---

# 20. Next scalability topics

```text
shared MongoDB bottlenecks
        ↓
connection pools
        ↓
Kafka consumer scaling vs partitions
        ↓
backpressure
        ↓
caching
        ↓
scalability failure modes / bottleneck propagation
```

---

[Back to README](../README.md) · [Project Roadmap](ROADMAP.md)
