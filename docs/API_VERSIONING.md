# API Versioning — Enrollment Service

Learning notes for API evolution, backward compatibility, OpenAPI-first versioning and deprecation in the Climbing Management project.

[Back to README](../README.md) · [Progress and next steps](ROADMAP.md)

Progress checkboxes belong only in `ROADMAP.md`. This document records the theory, implementation and tests used for the API-versioning milestone.

---

## 1. Why API versioning exists

An HTTP API is a contract between a service and its consumers.

An additive change can often remain compatible:

```text
existing field kept
+
new optional field added
=
existing clients can usually continue working
```

A breaking change alters something an existing client depends on, for example:

```text
field removed
field renamed
field type changed
request structure changed
response structure changed
semantics changed incompatibly
```

When consumers cannot all migrate at the same time, multiple API versions can coexist during a transition.

---

## 2. Common versioning strategies

Common approaches include:

```text
Path versioning
/api/v1/enrollments
/api/v2/enrollments

Header versioning
X-API-Version: 2

Media-type versioning
Accept: application/vnd.climbing.v2+json
```

This project uses **path versioning** because it is explicit, easy to exercise with Postman and MockMvc, and straightforward to represent in OpenAPI.

---

## 3. Current versioned endpoints

The Enrollment Service currently exposes:

```text
V1 — supported, deprecated
GET  /api/v1/enrollments
POST /api/v1/enrollments

V2 — current contract
GET  /api/v2/enrollments
POST /api/v2/enrollments
```

The previous unversioned route:

```text
/enrollments
```

is no longer mapped and is covered by regression tests expecting `404` for an authenticated request.

Authentication is included in that regression test so Spring Security does not turn the route-existence check into a `401` before MVC routing is evaluated.

---

## 4. OpenAPI remains the source of truth

The project remains API-first:

```text
openapi/enrollment-api.yaml
        ↓
OpenAPI Generator
        ↓
generated API interfaces + models
        ↓
REST inbound adapters
        ↓
application use cases
        ↓
domain
```

The versioned paths are declared in the OpenAPI contract rather than handwritten on controller methods.

The generator is configured with:

```xml
<useTags>true</useTags>
```

This is important because the `/api/...` path prefix otherwise caused the generated interface to be named `ApiApi`.

With OpenAPI tags:

```yaml
tags:
  - Enrollments
```

and:

```yaml
tags:
  - EnrollmentsV2
```

the generated interfaces remain meaningful:

```text
EnrollmentsApi
EnrollmentsV2Api
```

---

## 5. V1 and V2 intentionally use different HTTP request shapes

V1 keeps the original flat request:

```json
{
  "courseId": 10,
  "studentName": "Rubén"
}
```

V2 introduces an intentionally breaking representation:

```json
{
  "courseId": 10,
  "student": {
    "name": "Rubén"
  }
}
```

The V2 OpenAPI contract therefore introduces generated models equivalent to:

```text
EnrollmentV2Request
└── StudentV2
```

This is a contract change at the HTTP boundary.

It is **not** automatically a domain-model change.

---

## 6. API version does not leak into the domain

The domain still represents the same business concept:

```text
Enrollment
├── EnrollmentId
├── CourseId
├── StudentName
└── EnrollmentStatus
```

Both HTTP versions mean the same business command:

```text
Create an enrollment
for Course 10
for student "Rubén"
```

V1 translates:

```text
EnrollmentRequest.studentName
        ↓
CreateEnrollmentCommand.studentName
```

V2 translates:

```text
EnrollmentV2Request.student.name
        ↓
CreateEnrollmentCommand.studentName
```

Therefore both adapters call the same application port:

```text
CreateEnrollmentUseCase
```

and the same framework-free domain.

The architecture is:

```text
V1 HTTP contract ─→ EnrollmentController ─┐
                                         │
                                         ├─→ CreateEnrollmentUseCase
                                         │          ↓
V2 HTTP contract ─→ EnrollmentV2Controller┘       Domain
```

No `CreateEnrollmentV2UseCase` and no V2-specific domain model are needed because the business meaning did not change.

---

## 7. Separate inbound adapters, shared application logic

V1 is implemented by:

```text
EnrollmentController
implements EnrollmentsApi
```

V2 is implemented by:

```text
EnrollmentV2Controller
implements EnrollmentsV2Api
```

The controllers are allowed to understand their own generated HTTP DTOs.

The application and domain layers are not.

This preserves the dependency direction:

```text
generated OpenAPI classes → REST adapters
REST adapters             → application/domain
application               → no OpenAPI dependency
domain                    → no OpenAPI dependency
```

---

## 8. Security must evolve with versioned routes

Changing an endpoint path also changes the route seen by Spring Security.

The original security matchers targeted the unversioned path. After moving to `/api/v1/enrollments`, a USER POST initially fell through to the generic authenticated rule and reached the controller instead of returning `403`.

The security rules were therefore aligned with both generated API paths.

Conceptually:

```text
GET V1 or V2
    ↓
ROLE_USER or ROLE_ADMIN

POST V1 or V2
    ↓
ROLE_ADMIN
```

Generated path constants are reused by the security configuration so the OpenAPI-generated routes remain the source of truth instead of duplicating path strings.

---

## 9. Backward compatibility

Introducing V2 does not require immediately deleting V1.

The project currently demonstrates:

```text
V1 continues working
+
V2 is introduced
=
existing V1 clients can migrate gradually
```

This is the key difference between:

```text
versioning
```

and:

```text
simply replacing a breaking API contract
```

Both versions currently reach the same application/domain behavior.

---

## 10. Deprecation

V1 is still supported but is marked as deprecated in the OpenAPI contract:

```yaml
deprecated: true
```

This communicates lifecycle state to generated documentation and API tooling.

Successful V1 runtime responses also expose a:

```text
Deprecation
```

response header.

V2 responses do not contain that header.

The project therefore distinguishes:

```text
deprecated
=
still works, but consumers should migrate

removed
=
endpoint no longer exists
```

No `Sunset` date is documented because a concrete removal date has not been established.

---

## 11. MVC/security tests

Dedicated MVC slice tests cover both controllers.

V1 verifies, among other behavior:

```text
GET without authentication → 401
GET USER / ADMIN           → 200
POST without authentication→ 401
POST USER                  → 403
POST ADMIN                 → 201
V1 request mapping         → CreateEnrollmentCommand
validation failures        → 400
Deprecation header         → present
```

V2 verifies the equivalent security behavior plus the new request mapping:

```text
student.name
    ↓
CreateEnrollmentCommand.studentName
```

It also verifies:

```text
Deprecation header → absent
```

The old unversioned route is covered as:

```text
authenticated GET /enrollments
        ↓
404 Not Found
```

---

## 12. Full HTTP integration tests

The full integration suite keeps the Enrollment Service internals real:

```text
MockMvc
    ↓
SecurityFilterChain
    ↓
V1 / V2 Controller
    ↓
Application Service
    ↓
MongoEnrollmentAdapter
    ↓
EnrollmentRepository
    ↓
MongoDB Testcontainer
```

Only external boundaries remain mocked:

```text
JwtDecoder / identity infrastructure
CourseRestAdapter / external Course Service
```

A V2 integration test proves that the nested V2 request reaches the same application/domain path and persists the same Enrollment data as V1.

---

## 13. Important cross-cutting consequence: observability

API versioning changes the Micrometer HTTP `uri` label.

Before versioning, dashboards and SLO queries could filter on:

```promql
uri="/enrollments"
```

The active routes are now:

```text
/api/v1/enrollments
/api/v2/enrollments
```

Queries that still filter only on the old URI can return no data or silently stop describing current traffic.

The next cross-cutting task is therefore to review Prometheus/Grafana queries and decide whether each panel or SLO should:

```text
measure V1 separately
measure V2 separately
or aggregate both versions
```

This is a useful production lesson: changing an HTTP contract can affect security, tests, dashboards, SLOs, alerts, documentation and clients even when the domain model is unchanged.

---

## 14. Current architecture

```text
Client
  │
  ├── /api/v1/enrollments
  │       ↓
  │   EnrollmentController
  │
  └── /api/v2/enrollments
          ↓
      EnrollmentV2Controller
          │
          └──────────────┐
                         ↓
              CreateEnrollmentUseCase
              FindEnrollmentsUseCase
                         ↓
                 Application Services
                         ↓
                      Domain
                         ↓
                Outbound Ports
                         ↓
            Persistence / Course adapters
```

The central lesson is:

> API contracts may evolve independently at the HTTP boundary while stable application and domain concepts remain shared.
