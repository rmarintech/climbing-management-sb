# Climbing Management API — Infrastructure

Detailed containerization, CI/CD, Kubernetes, and Helm documentation for the Climbing Management API.

**Main README:** [README.md](README.md)  
**Backend:** [README-BACKEND.md](README-BACKEND.md)

---

## 🐳 Docker

Docker containerizes the complete backend environment and its infrastructure.

The application can run as a multi-container stack using Docker Compose:

### Docker Compose

```text
Docker Compose
      │
      ├───────────────┬───────────────┐
      ▼               ▼               ▼
 Spring Boot      PostgreSQL       MongoDB
   :8080             :5432           :27017
```

PostgreSQL and MongoDB communicate with the application through the Docker Compose network using service names:

postgres:5432
mongo:27017

The application exposes port 8080 to the host.

## Dockerfile

The application uses a multi-stage Docker build.

```text
Build stage
    │
    ├── Maven
    ├── JDK 21
    ├── Dependency resolution
    └── Application compilation
    │
    ▼
application JAR
    │
    ▼
Runtime stage
    │
    ├── Java 21 JRE
    ├── Alpine Linux
    └── application JAR
```

The Maven build environment is not included in the final runtime image.

This reduces:

Image size
Attack surface
Number of unnecessary production dependencies

The dependency layer is also separated from the source-code layer to improve Docker build-cache reuse.

## 🔐 Docker Security

The container is hardened using several production-oriented practices.

Non-root user

The Spring Boot application does not run as root.

RUN addgroup -S spring && adduser -S spring -G spring

USER spring

The container was verified to run as:

spring
Read-only root filesystem

The application container uses:

`read_only: true`

Temporary writable storage is provided through:

`tmpfs:`
- /tmp

This reduces the ability of a compromised application to modify the container filesystem.

## 🔑 Docker Secrets

Database credentials are not passed to the Spring Boot application as environment variables.

Instead, Docker Secrets are used.

**Conceptually:**

Docker Secret
```text
│
├──────────────► PostgreSQL
│
└──────────────► Spring Boot
```

PostgreSQL receives the secret through:

/run/secrets/postgres_password

The Spring Boot application receives it through:

/run/secrets/spring.datasource.password

Spring Boot imports the mounted secret using:

`spring.config.import=optional:configtree:/run/secrets/`

This maps the mounted filename to:

`spring.datasource.password`

The database password is therefore not exposed as an application environment variable.

## 🌐 Docker Networking

Docker Compose provides an internal DNS service.

Containers communicate using service names rather than localhost.

For example:

Spring Boot
```text
│
├── postgres:5432
│
└── mongo:27017
```

Inside the Spring Boot container:

localhost

refers to the Spring Boot container itself.

It does not refer to PostgreSQL or MongoDB.

This distinction is essential when troubleshooting containerized applications.

## ❤️ Docker Healthchecks

Spring Boot Actuator is used to expose application health information.

The health endpoint is:

`GET /actuator/health`

**Example response:**

`{`
"groups": [
"liveness",
"readiness"
],
"status": "UP"
`}`

Docker uses the endpoint as its container healthcheck:

`healthcheck:`
`test: ["CMD-SHELL", "wget -q --spider http://localhost:8080/actuator/health || exit 1"]`
`interval: 10s`
`timeout: 5s`
`retries: 5`
`start_period: 20s`

The start_period prevents normal application startup time from immediately causing the container to become unhealthy.

## 🩺 Health Probes

The project distinguishes between:

Liveness
`↓`
Is the application alive?

Readiness
`↓`
Is the application ready to receive traffic?

Startup
`↓`
Has the application finished starting?

These concepts are used directly by the Kubernetes deployment.

## 🛠️ Docker Troubleshooting

The project includes hands-on troubleshooting exercises covering:

`docker compose ps`
`docker compose logs`
`docker compose exec`
`docker inspect`

The troubleshooting workflow is:

`docker compose ps`
`↓`
Is the container running?
`↓`
`docker compose logs`
`↓`
What is the application reporting?
`↓`
`docker compose exec app sh`
`↓`
Can we inspect the container from inside?
`↓`
`docker inspect`
`↓`
What configuration was actually applied?

Docker DNS can also be verified from inside the application container:

getent hosts postgres
getent hosts mongo

A deliberate failure was introduced by changing the PostgreSQL hostname from:

postgres

to:

localhost

The resulting failure demonstrated that localhost inside a container refers to the container itself rather than another Compose service.

The issue was diagnosed through application logs, container inspection and Docker DNS verification.

## 🔍 Docker Vulnerability Scanning

The project uses Trivy to scan Docker images for known vulnerabilities.

**Example:**

`trivy image climbing-management-sb-app:latest`

The project demonstrated the difference between vulnerabilities in:

Application dependencies
vs
Base operating-system packages

A vulnerability scan initially detected critical vulnerabilities in:

org.apache.tomcat.embed:tomcat-embed-core

The dependency was traced through the Maven dependency tree:

Spring Boot
`↓`
spring-boot-starter-tomcat
`↓`
tomcat-embed-core

The Tomcat version was explicitly overridden to the fixed release.

The application JAR subsequently reported:

CRITICAL: 0

Remaining operating-system vulnerabilities were identified separately as Alpine base-image findings.

This demonstrates a practical vulnerability-management workflow:

Scan
`↓`
Identify severity
`↓`
Locate dependency
`↓`
Determine fixed version
`↓`
Update dependency
`↓`
Rebuild image
`↓`
Rescan
## 🧪 Docker + Spring Boot Configuration

Docker-specific Spring configuration is activated using:

`SPRING_PROFILES_ACTIVE: docker`

The Docker profile uses Compose service names:

`spring.datasource.url=jdbc:postgresql://postgres:5432/${APP_POSTGRES_DB}`

`spring.mongodb.uri=mongodb://mongo:27017/${APP_MONGO_DB}`

The configuration flow is:

.env
```text
│
├── ENV_POSTGRES_DB
├── ENV_POSTGRES_USER
├── ENV_MONGO_DB
└── ENV_APP_PORT
│
▼
```
### Docker Compose
```text
│
▼
```
Spring Boot environment
```text
│
▼
application-docker.properties
```

Secrets follow a separate path:

Docker Secret
```text
│
▼
```
/run/secrets/
```text
│
▼
```
Spring Boot configtree
```text
│
▼
spring.datasource.password
```
## ☸️ Kubernetes

The application and its database dependencies have been deployed to a local Kubernetes cluster running through Docker Desktop Kubernetes.

The Kubernetes environment currently consists of:

```text
Kubernetes Cluster
        │
        ▼
Namespace: climbing-management
        │
        ├──────────────────────┬──────────────────────┐
        ▼                      ▼                      ▼
Spring Boot              PostgreSQL               MongoDB
Deployment               Deployment               Deployment
        │                      │                      │
      2+ Pods                 1 Pod                  1 Pod
        │                      │                      │
        ▼                      ▼                      ▼
     Service                Service                Service
      :8080                  :5432                  :27017
```

The application is exposed externally through an Ingress layer:

```text
Client
  │
  ▼
NGINX Ingress Controller
  │
  ▼
Ingress rule
  │
  ▼
climbing-management-service
  │
  ├──► App Pod #1
  └──► App Pod #2
```

The Kubernetes resources are organized inside a dedicated namespace.

## 📁 Kubernetes Namespaces

The project uses Kubernetes Namespaces to provide logical isolation and scope for the application resources.

**Conceptually:**

```text
Kubernetes Cluster
   │
   ├── default
   │
   └── climbing-management
          │
          ├── Application Deployment
          ├── Application Service
          ├── PostgreSQL
          ├── MongoDB
          ├── ConfigMap
          ├── Secrets
          ├── PVC
          └── HPA
```

Namespaces provide an important organizational boundary for Kubernetes resources.

Instead of deploying the application into the default namespace, the project uses a dedicated namespace for the application environment.

This makes resource management and operational commands explicit:

`kubectl get pods -n climbing-management`
`kubectl get services -n climbing-management`
`kubectl get deployments -n climbing-management`
`kubectl get hpa -n climbing-management`

The namespace also becomes part of the Helm deployment context.

## 📦 Kubernetes Deployments

The application is deployed using a Kubernetes Deployment.

The baseline configuration uses:

`replicas: 2`

The application Deployment is also managed by an HPA with:

Minimum replicas: 2
Maximum replicas: 5
CPU target: 70%

This means the Deployment normally starts with two replicas but Kubernetes can automatically increase the number of Pods when CPU utilization exceeds the configured target.

**Conceptually:**

```text
Deployment
    │
    ▼
HPA controls replica count
    │
    ├───────────────┐
    ▼               ▼
Minimum: 2       Maximum: 5
    │
    ▼
Spring Boot Pods
```

The Deployment manages the Pods through a ReplicaSet.

If an application Pod is deleted or fails, Kubernetes creates a replacement to maintain the desired replica count.

This demonstrates Kubernetes self-healing and desired state management.

## 🌐 Kubernetes Services

The application is exposed internally through:

climbing-management-service

The Service uses:

`type: ClusterIP`

and exposes:

8080

The Service selects Pods using:

`selector:`
`app: climbing-management`

**Conceptually:**

```text
Service
climbing-management-service:8080
          │
          ├───────────┐
          ▼           ▼
      App Pod      App Pod
       :8080        :8080
```

The Service provides a stable network endpoint while Pods remain ephemeral.

Kubernetes dynamically maintains the Service's EndpointSlice based on matching and Ready Pods.

## 🔎 Kubernetes Service Discovery

Kubernetes provides internal DNS-based service discovery.

The Spring Boot application connects to PostgreSQL using:

postgres:5432

and MongoDB using:

mongo:27017

These names resolve to Kubernetes Services rather than directly to Pod IP addresses.

**Conceptually:**

Spring Boot Pod
```text
│
├── postgres:5432
│       ↓
│   PostgreSQL Service
│       ↓
│   PostgreSQL Pod
│
└── mongo:27017
↓
```
MongoDB Service
`↓`
MongoDB Pod

This is one of the key differences between container-level networking and Kubernetes service discovery.

## ⚙️ Kubernetes ConfigMap

Non-sensitive application configuration is stored in:

climbing-management-config

The ConfigMap contains values such as:

`SPRING_PROFILES_ACTIVE`
`APP_NAME`
LOG_LEVEL
`APP_MONGO_DB`
`APP_POSTGRES_USER`
`APP_POSTGRES_DB`

The application Deployment imports the ConfigMap using:

`envFrom:`
- configMapRef:
  `name: climbing-management-config`

This separates configuration from the container image.

## 🔐 Kubernetes Secrets

Sensitive configuration is stored in:

climbing-management-secret

The database password is mounted into the application Pod as a file:

/run/secrets/spring.datasource.password

The application does not require the password to be exposed as an environment variable.

The Deployment also disables automatic ServiceAccount token mounting:

`automountServiceAccountToken: false`

because the application does not need to communicate with the Kubernetes API.

This follows the principle of least privilege.

## 🩺 Kubernetes Health Probes

The Spring Boot Actuator endpoints are used by Kubernetes:

/actuator/health
/actuator/health/readiness
/actuator/health/liveness

The Deployment configures:

Startup probe

Determines whether the application has completed startup.

Startup
`↓`
Application initialization
Readiness probe

Determines whether the Pod should receive traffic.

If readiness fails:

Pod
`↓`
removed from Service endpoints

The container is not necessarily restarted.

Liveness probe

Determines whether the container should be restarted.

If liveness repeatedly fails:

Liveness failure
`↓`
Kubernetes restarts container

Interview summary:

Liveness determines whether Kubernetes should restart the container. Readiness determines whether the Pod should receive traffic. Startup probes protect slow-starting applications from being restarted by liveness before initialization has completed.

## 🔄 Kubernetes Rolling Updates

The application Deployment supports rolling updates.

When a new application version is deployed, Kubernetes creates new Pods while gradually replacing the old Pods.

**Conceptually:**

Old version
```text
│
├── Pod A
└── Pod B
↓
```
Rolling update
```text
↓
┌────┴────┐
▼         ▼
```
New Pod   New Pod

This reduces application downtime during deployments.

The Deployment also maintains revision history, allowing previous versions to be restored using Kubernetes rollout commands.

## 💾 Kubernetes Persistent Storage

PostgreSQL is configured with persistent storage so that database data survives Pod recreation.

The storage architecture is:

PostgreSQL Pod
```text
│
▼
```
PersistentVolumeClaim
```text
│
▼
```
PersistentVolume
```text
│
▼
```
StorageClass
```text
│
▼
```
Dynamically provisioned storage

The PostgreSQL PVC requests:

Access mode:
ReadWriteOnce

Storage:
requested by the PostgreSQL workload

The ReadWriteOnce access mode is appropriate for the single-node local PostgreSQL setup used in this project.

The important Kubernetes storage concepts are:

StorageClass
`↓`
Defines how storage is provisioned

PersistentVolume
`↓`
Represents provisioned storage

PersistentVolumeClaim
`↓`
Application request for storage

Pod
`↓`
Mounts the PVC

This separates the application's storage requirement from the underlying storage implementation.

## 📊 Kubernetes Resource Requests and Limits

The application Deployment defines CPU and memory resources.

Current application configuration:

CPU request:     250m
Memory request:  512Mi

CPU limit:       500m
Memory limit:    1Gi

**Conceptually:**

Request
`↓`
Resources reserved / used for scheduling decisions

Limit
`↓`
Maximum resource consumption allowed by the container

The CPU request is especially important for the HPA because CPU utilization is calculated relative to the configured CPU request.

For this application:

CPU request = 250m
HPA target   = 70%

250m × 70% = 175m

Therefore approximately 175m CPU utilization per Pod corresponds to the 70% HPA target.

Resource requests also allow Kubernetes to make better scheduling decisions across nodes.

## 📈 Kubernetes Metrics Server

The project uses Metrics Server to provide resource utilization metrics to Kubernetes.

Metrics can be inspected using:

`kubectl top nodes`

and:

`kubectl top pods`

Example conceptual output:

NAME                         CPU(cores)   MEMORY(bytes)
climbing-management-xxxxx    120m         350Mi
climbing-management-yyyyy    95m          340Mi

Metrics Server is required for the HPA to make CPU-based scaling decisions.

The architecture is:

Kubelet
```text
│
▼
```
Metrics Server
```text
│
▼
```
Kubernetes Metrics API
```text
│
▼
```
HPA
## 📈 Kubernetes Horizontal Pod Autoscaler

The application uses a Kubernetes Horizontal Pod Autoscaler.

The HPA configuration is:

Target:
climbing-management Deployment

Minimum replicas:
2

Maximum replicas:
5

CPU target:
70%

**Conceptually:**

Metrics Server
```text
│
▼
```
HPA
`│`
CPU utilization
```text
│
▼
```
Application
Deployment
```text
│
┌─────────┴─────────┐
▼                   ▼
```
App Pods           App Pods

When CPU utilization increases above the target, the HPA increases the number of application replicas.

When utilization decreases, Kubernetes can reduce the number of replicas, respecting the configured minimum.

The HPA manages the Deployment's replica count rather than creating Pods directly.

A simplified scaling calculation is:

desired replicas =
current replicas × current utilization / target utilization

For example:

2 replicas
146% CPU utilization
70% target

2 × 146 / 70
≈ 4.17

The HPA therefore needs approximately 5 replicas, subject to Kubernetes rounding and the configured maximum.

During load testing, the application demonstrated automatic scaling from:

2 replicas
`↓`
4 replicas
`↓`
5 replicas

This demonstrates practical Kubernetes horizontal scaling.

## 🌐 Kubernetes Ingress

The application is exposed through a Kubernetes Ingress.

Ingress provides Layer 7 HTTP/HTTPS routing.

The project uses:

- [x] NGINX Ingress Controller

The request flow is:

Client
```text
│
▼
```
- [x] NGINX Ingress Controller
```text
│
▼
```
Ingress rule
```text
│
▼
```
climbing-management-service:8080
```text
│
├──► App Pod #1
│
└──► App Pod #2
```

The Ingress resource contains the host:

climbing-management.local

and routes traffic to:

climbing-management-service:8080

The Ingress definition is stored in:

k8s/app-ingress.yaml

**Conceptually:**

Host:
climbing-management.local

Path:
/

Backend:
climbing-management-service:8080

Ingress is responsible for HTTP routing, while the Kubernetes Service remains responsible for stable internal access to the application Pods.

## 🚪 NGINX Ingress Controller

The project uses the NGINX Ingress Controller to implement the Kubernetes Ingress resource.

The controller watches Kubernetes Ingress resources and configures NGINX accordingly.

The distinction is:

Ingress
`↓`
Kubernetes routing configuration

Ingress Controller
`↓`
Actual component implementing the routing

The project installs the NGINX Ingress Controller and verifies that the controller Pod is running.

## 🧪 Kubernetes Ingress Local Testing

The Ingress was tested locally through a Kubernetes port-forward.

The controller Service is forwarded to the local machine:

`kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 8081:80`

The Ingress routing can then be tested with:

`curl.exe -H "Host: climbing-management.local" http://localhost:8081/actuator/health`

**Expected response:**

up

The complete request path is:

Windows host
```text
│
▼
```
localhost:8081
```text
│
▼
kubectl port-forward
│
▼
```
- [x] NGINX Ingress Controller
```text
│
▼
```
Ingress rule
```text
│
▼
```
climbing-management-service
```text
│
▼
```
Spring Boot Pod
```text
│
▼
```
/actuator/health

No Windows hosts-file modification is required for this local test because the HTTP Host header is supplied explicitly.

The port-forward is a development and testing mechanism, not a production ingress architecture.

## 🔌 Kubernetes Local Testing Without Ingress

The application can also be accessed directly through its Service using:

`kubectl port-forward service/climbing-management-service 8080:8080`

The request path becomes:

localhost:8080
```text
│
▼
kubectl port-forward
│
▼
```
Kubernetes Service
```text
│
├──► App Pod #1
│
└──► App Pod #2
```

Health can then be tested with:

`http://localhost:8080/actuator/health`

This is useful for debugging the Service and application independently of the Ingress layer.

## 🧩 Kubernetes Database Services

PostgreSQL is deployed internally as:

PostgreSQL Deployment
`↓`
PostgreSQL Service
`↓`
postgres:5432
`↓`
PostgreSQL Pod
`↓`
PVC

MongoDB is deployed internally as:

MongoDB Deployment
`↓`
MongoDB Service
`↓`
mongo:27017
`↓`
MongoDB Pod

Both database Services use ClusterIP, keeping the databases internal to the Kubernetes cluster.

PostgreSQL is backed by persistent storage through a PVC.

## ⛵ Helm

The project uses Helm to package and deploy the Kubernetes application.

Helm provides a packaging and templating layer above raw Kubernetes manifests.

Instead of maintaining a collection of independently applied YAML files, the Kubernetes application can be represented as a Helm Chart:

Helm Chart
```text
│
├── Chart.yaml
├── values.yaml
└── templates/
│
├── Deployment
├── Service
├── ConfigMap
├── Secret
├── HPA
└── Ingress
```

The relationship is:

values.yaml
```text
│
▼
```
- [x] Helm templates
```text
│
▼
```
Rendered Kubernetes manifests
```text
│
▼
```
Helm release
```text
│
▼
```
Kubernetes namespace

This provides a more maintainable deployment mechanism than manually applying each Kubernetes manifest independently.

## 🎛️ Helm Values

Configuration that changes between deployments can be parameterized through Helm values.

**Conceptually:**

`image:`
`repository: ghcr.io/rmarintech/climbing-management-sb`
`tag: <commit-sha>`

replicaCount: 2

The important principle is that the Kubernetes templates remain stable while deployment-specific configuration is supplied through values.

This allows the same chart structure to be reused with different:

image versions
replica counts
resources
environment configuration
Ingress configuration
## 🧩 Helm Templates

Helm templates generate the Kubernetes resources from reusable templates.

**Conceptually:**

values.yaml
```text
│
▼
```
{{ .Values.* }}
```text
│
▼
```
Kubernetes YAML

This avoids duplicating almost identical Kubernetes manifests for different image versions or deployment configurations.

The rendered output can be inspected before installation using:

`helm template ...`

This is useful for validating what Kubernetes resources Helm will generate.

## 📦 Helm Releases

A Helm deployment is tracked as a Helm release.

**Conceptually:**

Chart
+
Values
`↓`
Helm Release
`↓`
Kubernetes Resources

The release provides a higher-level deployment abstraction over the individual Kubernetes resources.

Typical operational commands include:

`helm list`
`helm status <release>`
`helm history <release>`
`helm upgrade ...`
`helm rollback ...`

This provides a deployment history and release-oriented lifecycle management on top of Kubernetes.

## 🔒 Immutable Image Deployment with Helm

The project uses the Docker image commit SHA as the image version deployed through Helm.

The image is published by CI as:

`ghcr.io/rmarintech/climbing-management-sb:<commit-sha>`

The Helm deployment references the immutable image tag rather than relying on:

latest

**Conceptually:**

Git commit
```text
│
▼
```
Commit SHA
```text
│
▼
```
Docker image
```text
│
▼
```
GHCR
```text
│
▼
```
- [x] Helm values
```text
│
▼
```
Kubernetes Deployment

For example:

commit:
abc123...

`image:`
`ghcr.io/rmarintech/climbing-management-sb:abc123...`

This provides deterministic deployment behavior.

The important difference is:

latest
`↓`
Mutable
`↓`
May point to a different image later

versus:

commit SHA
`↓`
Immutable reference
`↓`
Identifies one exact image build

This is particularly important in CI/CD because the image deployed to Kubernetes can be traced back to the exact Git commit that produced it.

## 🔄 Helm Deployment Strategy

The deployment flow now combines the CI/CD image pipeline with Helm:

Developer
```text
│
▼
```
Git commit
```text
│
▼
```
- GitHub Actions
```text
│
├── Maven build
├── Tests
├── JAR
└── Docker image
│
▼
```
GHCR
`│`
commit-SHA tag
```text
│
▼
```
## Helm
```text
│
helm upgrade
│
▼
```
## Kubernetes
```text
│
▼
```
Namespace
```text
│
┌─────┴─────┐
▼           ▼
```
App Pods    Services

This demonstrates an important modern deployment pattern:

Build artifact once
`↓`
Publish immutable image
`↓`
Reference exact image version
`↓`
Deploy through Helm
`↓`
Run in Kubernetes
## 🚚 Continuous Delivery with Helm

Helm provides the deployment abstraction for Kubernetes while GitHub Actions provides the CI/CD automation.

**Conceptually:**

```text
CI
 │
 ├── Build
 ├── Test
 └── Docker image
```
```text
└── Push to GHCR
│
▼
```
CD
```text
│
▼
```
```text
Helm
 │
 ▼
Kubernetes
```

The important separation is:

- GitHub Actions
  `↓`
  Automation

## Helm
`↓`
Kubernetes application packaging and release management

## Kubernetes
`↓`
Runtime orchestration

This makes the responsibilities of each technology explicit.

## 🔄 Helm Upgrade and Rollback

Helm supports controlled application upgrades.

**Conceptually:**

Release v1
```text
│
▼
helm upgrade
│
▼
```
Release v2

If a deployment needs to be reverted:

Release v2
```text
│
▼
helm rollback
│
▼
```
Release v1

Combined with immutable Docker image tags, the release history can identify exactly which application image was deployed.

This provides a useful foundation for controlled deployment and rollback strategies.

## 🔨 Continuous Integration

The CI workflow is triggered automatically when code is pushed to the main branch.

The current pipeline performs:

Git push
`↓`
- GitHub Actions
  `↓`
  Checkout repository
  `↓`
  Set up Java 21
  `↓`
  Start PostgreSQL service
  `↓`
  Start MongoDB service
  `↓`
  Run Maven build and tests
  `↓`
  Create JAR
  `↓`
  Upload JAR artifact
  `↓`
  Build Docker image
  `↓`
  Tag Docker image
  `↓`
  Push image to GHCR

The workflow is implemented in:

.github/workflows/ci.yml
## 🧪 GitHub Actions Service Containers

The CI environment provisions the external services required by the integration tests.

- [x] PostgreSQL
  `image: postgres:17`

The CI database is created as:

climbing_management
- [x] MongoDB
  `image: mongo:8`

Both services are configured with healthchecks so the workflow can verify that the databases are ready before the application tests run.

This is important because the GitHub-hosted runner does not use the user's local database installations.

## 📦 Maven CI Build

The workflow uses the Maven Wrapper:

`./mvnw package`

The Maven lifecycle used by the project includes:

`mvn test`
`↓`
compile + run tests

`mvn package`
`↓`
compile + test + create JAR

`mvn verify`
`↓`
package + additional verification

`mvn install`
`↓`
package + install JAR into local Maven repository

The CI pipeline currently uses:

`./mvnw package`

This ensures that the application is compiled, tested and packaged during CI.

## 📦 JAR Artifact

After the Maven build completes, the generated JAR is uploaded as a GitHub Actions artifact.

target/*.jar
`↓`
GitHub Actions Artifact

The artifact is named:

climbing-management-jar

This demonstrates the distinction between:

JAR
`↓`
Application build artifact

and:

Docker image
`↓`
Deployable application package
## 📦 GitHub Container Registry

The Docker image produced by CI is published to GitHub Container Registry (GHCR).

The image repository is:

`ghcr.io/rmarintech/climbing-management-sb`

The pipeline publishes two important tags:

`ghcr.io/rmarintech/climbing-management-sb:latest`

and an immutable commit-based tag:

`ghcr.io/rmarintech/climbing-management-sb:<commit-sha>`

The commit SHA tag identifies the exact source revision used to build the image.

This is preferable for deployments because:

latest
`↓`
Mutable reference

commit SHA
`↓`
Immutable version reference

The workflow authenticates to GHCR using the GitHub Actions GITHUB_TOKEN with package write permissions.

**Conceptually:**

GitHub Repository
```text
│
▼
```
- GitHub Actions
```text
│
├── Build
├── Test
└── Package
│
▼
```
Docker Image
```text
│
┌─────┴─────┐
▼           ▼
```
latest     commit SHA
```text
│           │
└─────┬─────┘
▼
```
GHCR
## 🚚 Continuous Delivery

The project also demonstrates a Continuous Delivery workflow.

The CD workflow runs after a successful CI workflow.

**Conceptually:**

CI succeeds
`↓`
CD workflow
`↓`
Production environment
`↓`
Required approval
`↓`
Helm deployment
`↓`
## Kubernetes

The production environment uses a GitHub Actions environment with required reviewers.

This demonstrates an important enterprise CI/CD concept:

A successful build does not automatically mean that production deployment should happen without an approval or deployment policy.

The deployment also uses the immutable Docker image commit SHA so that the deployed version can be identified precisely.

## 🔄 Build Once, Deploy Many

The CI/CD pipeline follows the principle:

Source code
`↓`
Build once
`↓`
Test once
`↓`
Create artifact/image
`↓`
Deploy the same version

The Docker image is built during CI and published to GHCR.

The resulting image can then be promoted through deployment environments without rebuilding the application.

This reduces the risk of:

Build version A
`↓`
Test version A
`↓`
Rebuild
`↓`
Deploy version B

Instead:

Build version A
`↓`
Test version A
`↓`
Deploy version A

Helm uses the immutable commit-SHA image reference to ensure that the Kubernetes deployment points to the exact image produced by CI.