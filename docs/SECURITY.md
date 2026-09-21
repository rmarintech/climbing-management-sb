# Spring Security — Enrollment Service

Learning notes for HTTP Basic, role-based access control, CSRF, stateless authentication, OAuth2, OpenID Connect, JWT, Keycloak and Bearer-token security in the Climbing Management project.

[Back to README](../README.md) · [Progress and next steps](ROADMAP.md)

Progress checkboxes belong only in ROADMAP.md. This guide records the theory and exercises covered so far; it is not a claim that every example has been implemented or verified.

## 1. Security at the application boundary

Spring Security protects incoming HTTP requests before they reach the REST controller. Domain objects and application use cases remain independent of HTTP authentication configuration.

Authentication establishes the caller's identity. Authorization determines whether that identity may perform an operation. CSRF protection is an additional request check; having the ADMIN role does not bypass it.

| Concern | Question | Enrollment example |
| --- | --- | --- |
| Authentication | Who is calling? | Initially validate Basic credentials; current runtime validates a Keycloak-issued Bearer JWT |
| Authorization | May this caller perform this operation? | Require ADMIN to create an enrollment |
| CSRF protection | Does a protected state-changing request supply the expected token? | Reject POST without a valid CSRF token when protection is enabled |
| Session policy | Is authentication retained between requests? | With STATELESS, do not retain it in an HTTP session |

## 2. HTTP Basic

The client supplies credentials in the request header:

```http
Authorization: Basic <base64(username:password)>
```

Base64 is encoding, not encryption. Use HTTPS when credentials travel over a network. Do not commit real passwords, copy live Authorization headers into documentation, or log them.

Basic authentication is enabled in the existing HttpSecurity chain with:

```java
.httpBasic(Customizer.withDefaults())
```

```java
import org.springframework.security.config.Customizer;
```

With stateless Basic authentication, credentials are validated on each request. There is no separate token-issuing login step in this exercise.

## 3. Roles and authorities

The learning users have these authorities:

| User | Authority | Enrollment policy |
| --- | --- | --- |
| `ruben` | `ROLE_USER` | Read enrollments |
| `admin` | `ROLE_ADMIN` | Read and create enrollments |

With the default role prefix, `hasRole("ADMIN")` checks for `ROLE_ADMIN`. Use `hasAuthority("ROLE_ADMIN")` when expressing the full authority directly. Do not pass `ROLE_ADMIN` to `hasRole`.

An ADMIN role does not automatically imply USER. The read rule explicitly allows either role.

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.GET, "/enrollments/**")
            .hasAnyRole("USER", "ADMIN")
        .requestMatchers(HttpMethod.POST, "/enrollments/**")
            .hasRole("ADMIN")
        .anyRequest()
            .authenticated()
)
```

```java
import org.springframework.http.HttpMethod;
```

Put specific matchers before the catch-all rule. The fallback above requires authentication for other requests; it does not make every other operation admin-only. Define permissions explicitly when adding endpoints. Preserve any existing deliberate health/probe rules when integrating these snippets.

## 4. Understanding status codes

| Status | Meaning in this exercise |
| --- | --- |
| `200 OK` | Authorized GET completed |
| `201 Created` | Authorized POST created an enrollment |
| `401 Unauthorized` | Missing or invalid credentials on a protected request |
| `403 Forbidden` | Insufficient permission, or rejection by CSRF protection |

A `403` alone does not identify which check failed. Inspect the request and configuration. With CSRF enabled, a missing-token POST can be rejected before the expected authentication/authorization response.

The user confirmed that enrollment creation returns `403` for `ruben` and `201` for `admin`.

## 5. CSRF: Cross-Site Request Forgery

A malicious site can cause a victim's browser to send an unwanted request to an application where the browser has credentials. The browser may attach cookies or cached HTTP Basic credentials automatically.

A CSRF token provides an additional value that the legitimate client explicitly sends and an external attacking site normally cannot obtain. Spring Security protects unsafe methods such as POST by default; GET should remain read-only.

Statelessness alone does not remove CSRF risk. Browser-managed Basic credentials and authentication cookies can still be automatically attached to requests. A JWT stored in an authentication cookie does not avoid that issue merely because it is a JWT.

### Local experiment

Temporarily replace:

```java
.csrf(csrf -> csrf.disable())
```

with:

```java
.csrf(Customizer.withDefaults())
```

Restart and use admin Basic credentials without supplying a CSRF token:

| Request | Expected result | Observed checkpoint |
| --- | --- | --- |
| `GET /enrollments` | `200` | Confirmed |
| `POST /enrollments` | `403` | Confirmed |

The rejected POST should not reach enrollment creation. ADMIN privileges do not bypass CSRF protection.

After the experiment, restore disabled CSRF for the scoped local non-browser API-client exercise. Do not generalize this to browser-facing applications. Decide CSRF policy from how clients send credentials, separately from session policy.

## 6. Explicit stateless authentication

The Enrollment Service explicitly uses:

```java
.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

```java
import org.springframework.security.config.http.SessionCreationPolicy;
```

Spring Security therefore does not create or use an HTTP session to persist authentication. This does not mean the application has no database, cache or business state.

| Policy | Authentication session behavior |
| --- | --- |
| `IF_REQUIRED` | Create a session when needed |
| `NEVER` | Do not create one, but an existing session can be used |
| `STATELESS` | Do not create or use one to retain the security context |

The stateless configuration was verified during the HTTP Basic exercise with CSRF disabled for the local non-browser client.

| Request | Authentication | Verified result |
| --- | --- | --- |
| `GET /enrollments` | Admin Basic credentials | `200` |
| `GET /enrollments` | None | `401` |
| `GET /enrollments` | Ruben Basic credentials | `200` |
| `POST /enrollments` | Ruben Basic credentials | `403` |
| `POST /enrollments` | Admin Basic credentials | `201` |
| `POST /enrollments` | None | `401` |

The later JWT Resource Server keeps the same stateless model: every protected request supplies its own Bearer access token.

## 7. OAuth2, OpenID Connect, Keycloak and JWT

The project then moved from the initial HTTP Basic exercise to token-based authentication.

### OAuth2 roles in this project

```text
User
  ↓
Postman
  ↓
Keycloak
  ↓
JWT access token
  ↓
Enrollment Service
```

| OAuth2 concept | Project component |
| --- | --- |
| Resource owner / end user | `ruben` or `admin` |
| OAuth2 client | Postman / `climbing-postman` |
| Authorization Server | Keycloak |
| Resource Server | Enrollment Service |
| Protected resource | `/enrollments` |

OAuth2 defines delegated authorization and token-based access. OpenID Connect adds an identity layer on top of OAuth2.

The local Keycloak realm is:

```text
climbing
```

Keycloak is available locally at:

```text
http://localhost:8083
```

Its OpenID Connect discovery document is:

```text
http://localhost:8083/realms/climbing/.well-known/openid-configuration
```

### Authorization Code + PKCE

Postman is registered as a public client:

```text
client_id = climbing-postman
```

The flow used is:

```text
Authorization Code + PKCE
```

Conceptually:

```text
Postman
   ↓
creates PKCE verifier + challenge
   ↓
browser login at Keycloak
   ↓
Keycloak authenticates user
   ↓
authorization code
   ↓
Postman exchanges code + verifier
   ↓
access token
```

Because this is a public client, no client secret is required.

### JWT access token

The access token is a JWT:

```text
header.payload.signature
```

Important claims verified in the project include:

```text
iss
aud
exp
preferred_username
realm_access.roles
```

The issuer is:

```text
http://localhost:8083/realms/climbing
```

The Enrollment Service is represented as an intended audience:

```text
enrollment-service
```

The observed audience was:

```json
"aud": [
  "enrollment-service",
  "account"
]
```

Keycloak realm roles are carried under:

```json
"realm_access": {
  "roles": [
    "USER",
    "ADMIN"
  ]
}
```

Access tokens expire. When an old token returns `401`, obtain a fresh token before troubleshooting authorization rules.

## 8. Audience and Resource Server validation

The OAuth2 client obtaining the token and the API receiving the token are different:

```text
climbing-postman
    =
OAuth2 client

enrollment-service
    =
protected API / audience
```

An Audience mapper was added to the Postman client's dedicated scope so new access tokens contain `enrollment-service` as an audience.

The Enrollment Service includes the OAuth2 Resource Server dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

Runtime configuration includes:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8083/realms/climbing
spring.security.oauth2.resourceserver.jwt.audiences=enrollment-service
```

Spring Security validates the JWT before the controller executes.

Important checks include:

```text
signature
issuer
expiration
audience
```

A token may therefore be correctly signed and still be rejected if it was not issued for this API.

## 9. Keycloak realm roles mapped to Spring authorities

Keycloak realm roles are not automatically equivalent to Spring Security roles.

The project reads:

```text
realm_access.roles
```

and converts them to Spring authorities:

```text
USER
    ↓
ROLE_USER

ADMIN
    ↓
ROLE_ADMIN
```

A custom `JwtAuthenticationConverter` is used for that conversion.

The important rule is:

```java
hasRole("ADMIN")
```

checks for:

```text
ROLE_ADMIN
```

The authorization policy remains:

```java
.requestMatchers(HttpMethod.GET, "/enrollments/**")
    .hasAnyRole("USER", "ADMIN")

.requestMatchers(HttpMethod.POST, "/enrollments/**")
    .hasRole("ADMIN")
```

Authentication and authorization are separate stages:

```text
Bearer JWT
    ↓
Authentication succeeds
    ↓
roles mapped to authorities
    ↓
authorization rule evaluated
```

A valid token can therefore still result in `403`.

## 10. Verified JWT / RBAC runtime matrix

The complete Bearer-token matrix was verified manually.

| Request | Authentication | Verified result |
| --- | --- | --- |
| `GET /enrollments` | No token | `401` |
| `GET /enrollments` | `ruben` JWT / USER | `200` |
| `GET /enrollments` | `admin` JWT / ADMIN | `200` |
| `POST /enrollments` | No token | `401` |
| `POST /enrollments` | `ruben` JWT / USER | `403` |
| `POST /enrollments` | `admin` JWT / ADMIN | `201` |

Interpretation:

```text
401 = authentication missing or invalid
403 = authenticated but not authorized
```

## 11. OpenAPI security alignment

The OpenAPI contract has now been aligned with the implemented Bearer-token model.

### Bearer security scheme

Under `components`:

```yaml
securitySchemes:
  bearerAuth:
    type: http
    scheme: bearer
    bearerFormat: JWT
```

`bearerFormat: JWT` is descriptive metadata. It does not configure Spring Security or perform runtime token validation.

### Operation security requirement

Both Enrollment operations declare:

```yaml
security:
  - bearerAuth: []
```

The empty array means no OAuth scopes are being declared by the OpenAPI operation. Authorization in the current application is based on Keycloak realm roles and Spring Security RBAC.

OpenAPI describes that a Bearer token is required. `SecurityConfiguration` still owns the concrete authorization rules:

```text
GET  → USER or ADMIN
POST → ADMIN
```

### Reusable `401` and `403` responses

Reusable responses were added under `components.responses`:

```yaml
responses:
  Unauthorized:
    description: Authentication is required or the access token is invalid

  Forbidden:
    description: The authenticated user does not have sufficient permissions
```

Operations reference them with:

```yaml
'401':
  $ref: '#/components/responses/Unauthorized'

'403':
  $ref: '#/components/responses/Forbidden'
```

The contract now represents the important security outcomes alongside the existing business responses.

```text
GET /enrollments
├── 200
├── 401
└── 403

POST /enrollments
├── 201
├── 400
├── 401
├── 403
├── 404
└── 503
```

The `401` and `403` entries document runtime behavior. They do not cause Spring Security to return those statuses; the Spring Security filter chain already enforces that behavior.

## 12. Maven, OpenAPI generation and the Docker build

The OpenAPI YAML is a real build input.

Running:

```powershell
.\mvnw clean verify
```

runs the Maven lifecycle phases that validate the OpenAPI contract, generate source code, compile the application, run tests and complete verification.

Conceptually:

```text
openapi/enrollment-api.yaml
        ↓
Maven OpenAPI validation
        ↓
OpenAPI Generator
        ↓
generated API interface / models
        ↓
compile / test / package / verify
```

The security additions in the YAML are therefore validated and consumed by the build, but they do not replace the Java Spring Security configuration.

```text
OpenAPI
→ API contract and generated boundary code

Spring Security
→ runtime JWT validation and RBAC enforcement
```

The Docker builder must also contain the OpenAPI contract before Maven runs.

The Enrollment Service Dockerfile therefore copies:

```dockerfile
COPY openapi ./openapi
```

in addition to:

```dockerfile
COPY src ./src
```

Without that copy, the containerized Maven build cannot find:

```text
/app/openapi/enrollment-api.yaml
```

and CI fails during the Enrollment image build.

This failure was observed, fixed, and the CI pipeline returned to green.

## 13. Security request flow

```text
User
    ↓
Keycloak login
    ↓
Authorization Code + PKCE
    ↓
Postman receives access token
    ↓
Authorization: Bearer <JWT>
    ↓
Enrollment Service
    ↓
Spring Security Resource Server
    ↓
JWT signature / issuer / expiration / audience
    ↓
JwtAuthenticationConverter
    ↓
realm_access.roles
    ↓
ROLE_USER / ROLE_ADMIN
    ↓
RBAC authorization
    ↓
EnrollmentController
```

## 14. Interview questions

**What is the difference between authentication and authorization?**

Authentication identifies a caller. Authorization decides which actions that authenticated caller may perform.

**What is the difference between OAuth2 and OpenID Connect?**

OAuth2 is an authorization framework. OpenID Connect adds an identity layer on top of OAuth2.

**Why use Authorization Code + PKCE for a public client?**

PKCE binds the authorization request to the later token exchange using a verifier/challenge pair. A public client such as Postman does not need to store a client secret.

**What does a Resource Server do?**

It protects API resources, receives Bearer access tokens, validates them and makes authenticated authorities available for authorization decisions.

**Why validate `aud` as well as `iss`?**

`iss` identifies who issued the token. `aud` identifies the intended recipient. Audience validation prevents accepting a valid token intended for a different API.

**Why is a custom JWT converter needed?**

Keycloak realm roles are stored under `realm_access.roles`. The converter maps them into Spring authorities such as `ROLE_USER` and `ROLE_ADMIN`.

**Can an authenticated user receive 403?**

Yes. A valid JWT establishes authentication, but the user may lack the authority required by the endpoint.

**Does `bearerFormat: JWT` make Spring validate JWTs?**

No. It documents the OpenAPI scheme. Runtime validation comes from Spring Security Resource Server configuration.

**Does adding `401` and `403` to OpenAPI make Spring return them?**

No. The contract describes those outcomes. Spring Security's filter chain produces the runtime responses.

**Why must the Dockerfile copy `openapi/`?**

Because the OpenAPI contract is used during the Maven build. The Docker builder needs the YAML before Maven can generate and compile the API sources.

**Does STATELESS mean no persistence?**

No. It means Spring Security does not persist authentication in an HTTP session. Business data can still be persisted normally.

## 15. Current checkpoint

The Security phase is complete for the planned course scope:

```text
HTTP Basic / RBAC / CSRF             ✅
Explicit stateless authentication    ✅
OAuth2 fundamentals                  ✅
OpenID Connect fundamentals          ✅
Keycloak                             ✅
Authorization Code + PKCE            ✅
JWT Resource Server                  ✅
Issuer validation                    ✅
Audience validation                  ✅
Realm-role mapping                   ✅
JWT RBAC runtime matrix              ✅
OpenAPI Bearer security scheme       ✅
OpenAPI 401 / 403 responses          ✅
Maven contract verification          ✅
Containerized OpenAPI build input    ✅
```

## References

- [Spring Security: CSRF concepts and browser credentials](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html)
- [Spring Security: servlet CSRF configuration](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security: authentication persistence and stateless sessions](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
- [Spring Security: OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
