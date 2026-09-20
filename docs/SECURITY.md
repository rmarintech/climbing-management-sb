# Spring Security — Enrollment Service

Learning notes for HTTP Basic, role-based access control, CSRF, stateless authentication, OAuth2, OpenID Connect, JWT, Keycloak and Spring Security Resource Server configuration in the Climbing Management project.

[Back to README](../README.md) · [Progress and next steps](ROADMAP.md)

Progress checkboxes belong only in ROADMAP.md. This guide records the theory, configuration and exercises covered so far.

## 1. Security at the application boundary

Spring Security protects incoming HTTP requests before they reach the REST controller. Domain objects and application use cases remain independent of HTTP authentication configuration.

Authentication establishes the caller's identity. Authorization determines whether that identity may perform an operation. CSRF protection is a separate request-protection concern.

The project has now exercised two authentication approaches:

```text
Initial learning step
HTTP Basic
    ↓
username + password on every request

Current implementation
OAuth2 / OpenID Connect + Keycloak
    ↓
Bearer JWT on every API request
```

| Concern | Question | Enrollment example |
| --- | --- | --- |
| Authentication | Who is calling? | Validate a JWT issued by the `climbing` Keycloak realm |
| Authorization | May this caller perform this operation? | Require ADMIN to create an enrollment |
| Token validation | May this API trust this token? | Validate signature, issuer, expiration and audience |
| Session policy | Is authentication retained between requests? | With `STATELESS`, every request supplies its own credentials/token |
| CSRF protection | Can a browser be tricked into sending an authenticated request? | Evaluate according to how credentials are transported |

## 2. HTTP Basic — first security exercise

The first security exercise used HTTP Basic.

The client supplied credentials in the request header:

```http
Authorization: Basic <base64(username:password)>
```

Base64 is encoding, not encryption. HTTPS is required when credentials travel over a network.

Basic authentication was enabled with:

```java
.httpBasic(Customizer.withDefaults())
```

```java
import org.springframework.security.config.Customizer;
```

With stateless HTTP Basic, credentials are validated on each request. There is no separate token-issuing login step.

This stage established the core distinction between:

```text
Authentication
    =
Who are you?

Authorization
    =
What are you allowed to do?
```

It also established the Enrollment API role policy before moving to JWT-based authentication.

## 3. Roles and authorities

The learning policy is:

| User | Keycloak realm role | Spring authority | Enrollment policy |
| --- | --- | --- | --- |
| `ruben` | `USER` | `ROLE_USER` | Read enrollments |
| `admin` | `ADMIN` | `ROLE_ADMIN` | Read and create enrollments |

The GET rule allows both USER and ADMIN explicitly:

```java
.requestMatchers(HttpMethod.GET, "/enrollments/**")
    .hasAnyRole("USER", "ADMIN")
```

The POST rule requires ADMIN:

```java
.requestMatchers(HttpMethod.POST, "/enrollments/**")
    .hasRole("ADMIN")
```

With Spring Security's default role prefix:

```java
hasRole("ADMIN")
```

checks for:

```text
ROLE_ADMIN
```

Therefore, when Keycloak supplies:

```text
ADMIN
```

the JWT role converter maps it to:

```text
ROLE_ADMIN
```

An ADMIN role does not automatically imply USER. The GET rule deliberately accepts either role.

## 4. Understanding 200, 201, 401 and 403

| Status | Meaning in this project |
| --- | --- |
| `200 OK` | Authorized GET completed |
| `201 Created` | Authorized POST created an enrollment |
| `401 Unauthorized` | Authentication is missing or the Bearer token cannot be accepted |
| `403 Forbidden` | Authentication succeeded, but the caller lacks the required authority; CSRF can also cause 403 when enabled |

Typical JWT-related reasons for `401` include:

```text
No Bearer token
Expired token
Invalid signature
Wrong issuer
Wrong audience
Malformed token
```

Typical authorization example:

```text
ruben
+
valid JWT
+
ROLE_USER
+
POST requires ROLE_ADMIN
=
403 Forbidden
```

The project has verified the security distinction in practice.

## 5. CSRF: Cross-Site Request Forgery

A malicious site can cause a victim's browser to send an unwanted request to an application where the browser automatically attaches credentials.

A CSRF token provides an additional value that the legitimate client explicitly sends and an external attacking site normally cannot obtain.

During the HTTP Basic learning exercise, CSRF was temporarily enabled:

```java
.csrf(Customizer.withDefaults())
```

The verified behavior was:

| Request | Authentication | Result |
| --- | --- | --- |
| `GET /enrollments` | admin Basic credentials | `200` |
| `POST /enrollments` | admin Basic credentials, no CSRF token | `403` |

ADMIN privileges do not bypass CSRF protection.

For the scoped non-browser API exercise, CSRF was then disabled again:

```java
.csrf(csrf -> csrf.disable())
```

CSRF policy and session policy are separate decisions. Statelessness alone does not automatically remove CSRF risk; the relevant question is whether the browser automatically attaches the authentication credential.

## 6. Explicit stateless authentication

The Enrollment Service explicitly uses a stateless security policy:

```java
.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

```java
import org.springframework.security.config.http.SessionCreationPolicy;
```

`STATELESS` means Spring Security does not use an HTTP session to retain the security context between requests.

It does **not** mean:

```text
No database
No MongoDB
No application state
No persisted business data
```

It means:

```text
Request A authenticates itself
Request B authenticates itself again
```

The complete Basic-auth role matrix was verified before the project moved to OAuth2/JWT.

## 7. OAuth2 and OpenID Connect fundamentals

The current security implementation introduces Keycloak as the authorization server / identity provider.

The roles are:

```text
User
    ↓
Keycloak
Authorization Server / Identity Provider
    ↓
Postman
OAuth2 Client
    ↓
Enrollment Service
OAuth2 Resource Server
```

OAuth2 is primarily about delegated authorization and access tokens.

OpenID Connect adds an identity layer on top of OAuth2.

In this project:

```text
Keycloak authenticates the user
    ↓
Keycloak issues tokens
    ↓
Postman obtains an access token
    ↓
Postman sends the access token to the Enrollment API
    ↓
Spring Security validates it
```

The Enrollment Service does not receive the user's Keycloak password. It receives a Bearer access token.

## 8. Local Keycloak setup

Keycloak runs locally on port `8083`:

```powershell
docker run -d --name climbing-keycloak `
  -p 127.0.0.1:8083:8080 `
  -e KC_BOOTSTRAP_ADMIN_USERNAME=kcadmin `
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=local-dev-only `
  -v climbing-keycloak-data:/opt/keycloak/data `
  quay.io/keycloak/keycloak:26.7.4 start-dev
```

Local console:

```text
http://localhost:8083/
```

Realm:

```text
climbing
```

OIDC discovery document:

```text
http://localhost:8083/realms/climbing/.well-known/openid-configuration
```

The realm contains the learning users and roles used by the Enrollment Service.

## 9. Postman OAuth2 client and Authorization Code + PKCE

A public Keycloak client was registered for Postman:

```text
Client ID: climbing-postman
Client type: OpenID Connect
Client authentication: Off
Standard flow: On
PKCE method: S256
```

Redirect URI:

```text
https://oauth.pstmn.io/v1/browser-callback
```

Postman uses:

```text
Authorization Code (With PKCE)
```

with:

```text
Auth URL:
http://localhost:8083/realms/climbing/protocol/openid-connect/auth

Access Token URL:
http://localhost:8083/realms/climbing/protocol/openid-connect/token
```

The flow is:

```text
Postman creates PKCE verifier/challenge
    ↓
Browser opens Keycloak
    ↓
User logs in
    ↓
Keycloak returns authorization code
    ↓
Postman sends code + PKCE verifier
    ↓
Keycloak returns access token
```

Because `climbing-postman` is a public client, it does not require a client secret.

A fresh token must be obtained after changing roles, audience mappers or relevant Keycloak client configuration.

## 10. Audience: client obtaining the token vs API receiving it

The OAuth client and protected API are different concepts:

```text
climbing-postman
    =
client obtaining the token

enrollment-service
    =
API intended to receive the token
```

The Postman client's dedicated scope contains an Audience mapper that adds:

```text
enrollment-service
```

to the access token.

The verified JWT audience is:

```json
"aud": [
  "enrollment-service",
  "account"
]
```

This allows the Enrollment Service to reject tokens that were not intended for it.

## 11. JWT structure and useful claims

A JWT consists of three Base64URL-encoded parts:

```text
header.payload.signature
```

Important claims used in this exercise include:

```text
iss
    issuer

aud
    intended audience

exp
    expiration

preferred_username
    authenticated user

realm_access.roles
    Keycloak realm roles
```

A verified token for `ruben` contained values equivalent to:

```text
iss = http://localhost:8083/realms/climbing
aud = enrollment-service, account
preferred_username = ruben
```

Realm roles are available under:

```json
"realm_access": {
  "roles": [
    "USER"
  ]
}
```

or, for an administrator, include `ADMIN`.

Tokens expire. A token that previously returned `200` can later return `401` because its `exp` time has passed.

## 12. Spring Boot OAuth2 Resource Server

The Enrollment Service includes the OAuth2 Resource Server dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

The resource server trusts the `climbing` realm issuer:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8083/realms/climbing
```

It also requires the intended API audience:

```properties
spring.security.oauth2.resourceserver.jwt.audiences=enrollment-service
```

Conceptually, Spring validates:

```text
Bearer JWT
    ↓
signature
    ↓
issuer
    ↓
expiration
    ↓
audience
```

Only after successful authentication does authorization continue.

## 13. Mapping Keycloak realm roles to Spring Security authorities

Keycloak stores realm roles in:

```text
realm_access.roles
```

Spring Security needs those values converted into granted authorities.

The project uses a `JwtAuthenticationConverter` / granted-authorities converter so that:

```text
USER
    ↓
ROLE_USER

ADMIN
    ↓
ROLE_ADMIN
```

The resource server configuration then uses that converter:

```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt
        .jwtAuthenticationConverter(jwtAuthenticationConverter)
    )
)
```

This bridges the Keycloak token model and the existing Spring RBAC rules.

## 14. Current authorization flow

The current flow is:

```text
HTTP request
    ↓
Authorization: Bearer <JWT>
    ↓
Spring Security Resource Server
    ↓
Validate JWT
    ├── signature
    ├── issuer
    ├── expiration
    └── audience
    ↓
JwtAuthenticationConverter
    ↓
realm_access.roles
    ↓
ROLE_USER / ROLE_ADMIN
    ↓
Authorization rules
    ↓
Controller
```

The domain and application layers remain independent of Keycloak and Spring Security.

## 15. Verified Bearer-token behavior

The JWT/RBAC flow has now been exercised successfully.

Verified checkpoints include:

```text
No token
    ↓
GET /enrollments
    ↓
401 Unauthorized
```

```text
ruben + valid token + USER role
    ↓
GET /enrollments
    ↓
200 OK
```

The ADMIN user was also tested and the protected behavior worked as intended.

The important distinction is:

```text
401
    =
authentication failed or is absent

403
    =
authentication succeeded but authorization denied the operation
```

## 16. Troubleshooting notes

### Expired token

If a request that previously worked begins returning `401`, obtain a fresh access token and inspect `exp`.

### Audience missing

After adding or changing the Keycloak Audience mapper, previously issued tokens do not change. Obtain a new token and verify that `aud` contains:

```text
enrollment-service
```

### Browser login appears to use the wrong user

An existing Keycloak browser session can reuse the previous login. Log out or use a private/incognito window before obtaining a token for another user.

### Port 8081 already in use

Find the listening process:

```powershell
netstat -ano | findstr :8081
```

Inspect it:

```powershell
Get-Process -Id <PID>
```

Terminate it if it is a stale Enrollment Service process:

```powershell
Stop-Process -Id <PID> -Force
```

or:

```powershell
taskkill /PID <PID> /F
```

Then restart the service.

## 17. Security and the existing OpenAPI contract

The existing business responses remain relevant:

```text
GET
└── 200

POST
├── 201
├── 400
├── 404
└── 503
```

Security additionally introduces:

```text
401
403
```

The remaining security task is to align the OpenAPI contract with the runtime security model:

```text
Bearer security scheme
    ↓
operation security requirements
    ↓
401 / 403 responses
```

Do not assume filter-level Spring Security failures automatically use the application's generated `ErrorResponse`; consistent security error bodies require deliberate handling.

## 18. Interview questions

**What is the difference between authentication and authorization?**

Authentication identifies a caller. Authorization decides which actions that authenticated caller may perform.

**What role does Keycloak have here?**

Keycloak is the authorization server / identity provider. It authenticates users and issues tokens.

**What role does the Enrollment Service have?**

It is an OAuth2 Resource Server. It receives and validates Bearer access tokens before serving protected resources.

**Why does Postman use Authorization Code with PKCE?**

Postman is configured as a public client, so PKCE protects the authorization-code flow without requiring a client secret.

**What is the difference between `climbing-postman` and `enrollment-service`?**

`climbing-postman` is the OAuth client requesting the token. `enrollment-service` is the protected API and appears as an intended token audience.

**What does `issuer-uri` protect?**

It tells Spring Security which issuer is trusted. A token from a different issuer is not accepted as a valid token for this resource server.

**Why validate the audience?**

A valid token issued by the trusted authorization server should still be rejected when it was intended for a different API.

**Why is a custom JWT authority converter needed?**

Keycloak realm roles are nested in `realm_access.roles`. The converter maps them to Spring authorities such as `ROLE_USER` and `ROLE_ADMIN`.

**Can an authenticated user receive 403?**

Yes. The JWT can be valid while the user lacks the authority required for the requested operation.

**What typically causes 401 with JWT authentication?**

Missing token, expired token, invalid signature, wrong issuer, wrong audience or another token-validation failure.

**Does `STATELESS` mean no persistence?**

No. It concerns security-context persistence in HTTP sessions. MongoDB and other business persistence continue normally.

**Can a stateless application still have CSRF concerns?**

Yes, depending on how credentials are transported. The relevant question is whether the browser automatically attaches the authentication credential.

**Why keep security configuration outside the domain model?**

Authentication and HTTP authorization are infrastructure-boundary concerns. The domain model remains focused on business rules and invariants.

## 19. Current checkpoint and next topic

Completed security learning checkpoints now include:

```text
HTTP Basic
RBAC
CSRF experiment
STATELESS sessions
OAuth2 fundamentals
OpenID Connect fundamentals
JWT structure / claims
Keycloak
Authorization Code + PKCE
Audience mapping
Spring OAuth2 Resource Server
Issuer validation
Audience validation
Keycloak realm-role conversion
Bearer-token RBAC verification
```

Next security task:

```text
Align OpenAPI with Bearer authentication and 401 / 403 responses
```

A suitable code checkpoint for the JWT/RBAC milestone is:

```bash
git commit -m "feat(security): secure enrollment API with Keycloak JWT RBAC"
```

Documentation checkpoint:

```bash
git add README.md docs/ROADMAP.md docs/SECURITY.md
git diff --cached
git commit -m "docs(security): document Keycloak OAuth2 JWT resource server"
```

## References

- [Spring Security: CSRF concepts and browser credentials](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html)
- [Spring Security: servlet CSRF configuration](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security: authentication persistence and stateless sessions](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
