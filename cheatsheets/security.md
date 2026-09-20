SECURITY COMMANDS — CLIMBING MANAGEMENT
=========================================

0. START KEYCLOAK
   =================

Run Keycloak locally:

```powershell
docker run -d --name climbing-keycloak `
  -p 127.0.0.1:8083:8080 `
  -e KC_BOOTSTRAP_ADMIN_USERNAME=kcadmin `
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=local-dev-only `
  -v climbing-keycloak-data:/opt/keycloak/data `
  quay.io/keycloak/keycloak:26.7.4 start-dev
```

Useful Docker commands:

```powershell
docker ps
docker ps -a
docker logs climbing-keycloak
docker start climbing-keycloak
docker stop climbing-keycloak
```

Open Keycloak:

```text
http://localhost:8083/
```

Login:

```text
username: kcadmin
password: local-dev-only
```

Create the realm:

```text
climbing
```

OIDC discovery document:

```text
http://localhost:8083/realms/climbing/.well-known/openid-configuration
```

This document exposes the realm's OAuth2 / OpenID Connect endpoints and metadata.


1. CREATE REALM ROLES AND USERS
   ===============================

Inside realm:

```text
climbing
```

Create realm roles, for example:

```text
USER
ADMIN
```

Create users, for example:

```text
ruben
admin
```

For every user:

```text
Users
→ <user>
→ Credentials
→ Set password
```

Then assign realm roles:

```text
Users
→ <user>
→ Role mapping
→ Assign role
```

Example:

```text
ruben → USER
admin → USER + ADMIN
```

The roles are stored in the JWT under:

```json
"realm_access": {
  "roles": [
    "USER",
    "ADMIN"
  ]
}
```


2. REGISTER POSTMAN IN KEYCLOAK
   ===============================

In the `climbing` realm:

```text
Clients
→ Create client
```

Configuration:

```text
Client type:                  OpenID Connect
Client ID:                    climbing-postman
Client authentication:       Off
Authorization:                Off
Standard flow:                On
Direct access grants:         Off
Other authentication flows:  Off
```

Login settings:

```text
Valid redirect URIs:
https://oauth.pstmn.io/v1/browser-callback
```

Leave these empty:

```text
Root URL
Home URL
Web origins
```

Configure PKCE:

```text
Clients
→ climbing-postman
→ Advanced settings
→ PKCE method
→ S256
```

This makes Postman a public OAuth2 client using:

```text
Authorization Code + PKCE
```

No client secret is required.


3. CONFIGURE POSTMAN OAUTH2
   ===========================

In Postman:

```text
Authorization
→ OAuth 2.0
```

Configure a new token:

```text
Token Name:             climbing-ruben
Grant Type:             Authorization Code (With PKCE)
Authorize using browser: Enabled
Callback URL:           https://oauth.pstmn.io/v1/browser-callback
Client ID:              climbing-postman
Client Secret:          <empty>
Scope:                  openid profile
Code Challenge Method:  SHA-256
Code Verifier:          <leave empty>
Client Authentication:  Send client credentials in body
```

Authorization URL:

```text
http://localhost:8083/realms/climbing/protocol/openid-connect/auth
```

Access Token URL:

```text
http://localhost:8083/realms/climbing/protocol/openid-connect/token
```

The callback URL must exactly match the URI registered in Keycloak.


4. OBTAIN AN ACCESS TOKEN
   =========================

In Postman:

```text
Get New Access Token
```

Browser flow:

```text
Postman
   ↓
Keycloak login
   ↓
User authenticates
   ↓
Keycloak returns authorization code
   ↓
Postman sends code + PKCE verifier
   ↓
Keycloak returns access token
```

Login as:

```text
ruben
```

or:

```text
admin
```

After obtaining the token:

```text
Use Token
```

IMPORTANT:

A previously issued token does not change when Keycloak configuration changes.

After changing:

```text
roles
audience
mappers
client configuration
```

always obtain a fresh token.


5. CONFIGURE THE TOKEN AUDIENCE
   ===============================

The OAuth client and the protected API are different concepts:

```text
climbing-postman
    =
client obtaining the token

enrollment-service
    =
API intended to receive the token
```

Configure the audience mapper:

```text
Clients
→ climbing-postman
→ Client scopes
→ climbing-postman-dedicated
→ Add mapper
→ By configuration
→ Audience
```

Configuration:

```text
Name:                       enrollment-audience
Included Client Audience:   <empty>
Included Custom Audience:   enrollment-service
Add to access token:        On
Add to ID token:            Off
```

Obtain a NEW token afterwards.

Expected JWT audience:

```json
"aud": [
  "enrollment-service",
  "account"
]
```

A single audience may also appear as a string.


6. INSPECT THE JWT IN POWERSHELL
   ================================

If the access token is stored in:

```powershell
$token
```

Decode the payload:

```powershell
$payload = $token.Split('.')[1]

switch ($payload.Length % 4) {
    2 { $payload += '==' }
    3 { $payload += '=' }
}

$payload = $payload.Replace('-', '+').Replace('_', '/')

$claims = [System.Text.Encoding]::UTF8.GetString(
    [Convert]::FromBase64String($payload)
) | ConvertFrom-Json
```

Inspect useful claims:

```powershell
$claims | Select-Object iss, aud, preferred_username
```

Expected example:

```text
iss                                   aud                           preferred_username
---                                   ---                           ------------------
http://localhost:8083/realms/climbing {enrollment-service, account} ruben
```

Inspect realm roles:

```powershell
$claims.realm_access.roles
```

Example:

```text
USER
ADMIN
```


7. SPRING BOOT RESOURCE SERVER DEPENDENCY
   =========================================

`pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

The Enrollment Service acts as an OAuth2 Resource Server.

It does NOT authenticate username/password itself.

It receives a Bearer token and validates it.


8. RESOURCE SERVER CONFIGURATION
   ================================

`application.properties`:

```properties
# Keycloak realm trusted to issue access tokens.
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8083/realms/climbing

# This API must be an intended recipient of the access token.
spring.security.oauth2.resourceserver.jwt.audiences=enrollment-service
```

Spring validates the token signature using the public keys exposed by Keycloak.

Important JWT validations include:

```text
signature
issuer (iss)
expiration (exp)
audience (aud)
```


9. MAP KEYCLOAK REALM ROLES TO SPRING AUTHORITIES
   =================================================

Keycloak realm roles are normally found here:

```json
"realm_access": {
  "roles": [
    "USER",
    "ADMIN"
  ]
}
```

Spring Security does not automatically interpret those roles as:

```text
ROLE_USER
ROLE_ADMIN
```

A custom `JwtAuthenticationConverter` is therefore used.

Example:

```java
@Bean
JwtAuthenticationConverter jwtAuthenticationConverter() {

    JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(jwt -> {

        Map<String, Object> realmAccess =
                jwt.getClaim("realm_access");

        if (realmAccess == null) {
            return List.of();
        }

        Object rolesObject = realmAccess.get("roles");

        if (!(rolesObject instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .map(Object::toString)
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
    });

    return converter;
}
```

Mapping:

```text
Keycloak role USER
        ↓
ROLE_USER

Keycloak role ADMIN
        ↓
ROLE_ADMIN
```

Why add `ROLE_`?

Because:

```java
hasRole("ADMIN")
```

internally checks for:

```text
ROLE_ADMIN
```


10. SECURITYFILTERCHAIN
    =======================

Example:

```java
@Bean
SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationConverter jwtAuthenticationConverter
) throws Exception {

    http
        .authorizeHttpRequests(auth -> auth

            // Example RBAC rules.
            .requestMatchers(HttpMethod.GET, "/enrollments/**")
                .hasAnyRole("USER", "ADMIN")

            .requestMatchers(HttpMethod.POST, "/enrollments/**")
                .hasRole("ADMIN")

            .requestMatchers(HttpMethod.PUT, "/enrollments/**")
                .hasRole("ADMIN")

            .requestMatchers(HttpMethod.DELETE, "/enrollments/**")
                .hasRole("ADMIN")

            .anyRequest()
                .authenticated()
        )

        // Authenticate requests using a validated JWT access token.
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(
                    jwtAuthenticationConverter
                )
            )
        );

    return http.build();
}
```

Conceptually:

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


11. AUTHENTICATION VS AUTHORIZATION
    ===================================

Authentication answers:

```text
Who are you?
```

Example:

```text
JWT says:
preferred_username = ruben
```

Authorization answers:

```text
What are you allowed to do?
```

Example:

```text
ROLE_USER
→ GET allowed

ROLE_ADMIN
→ GET / POST / PUT / DELETE allowed
```


12. TEST RUBEN
    ==============

Obtain a fresh token while logged into Keycloak as:

```text
ruben
```

Use the token in Postman.

GET:

```http
GET http://localhost:8081/enrollments
Authorization: Bearer <RUBEN_TOKEN>
```

Expected:

```text
200 OK
```

POST:

```http
POST http://localhost:8081/enrollments
Authorization: Bearer <RUBEN_TOKEN>
Content-Type: application/json
```

Expected:

```text
403 Forbidden
```

Reason:

```text
Ruben is authenticated
but does not have ROLE_ADMIN.
```


13. TEST ADMIN
    ==============

Obtain a fresh token while logged into Keycloak as:

```text
admin
```

Verify that the token contains:

```text
ADMIN
```

under:

```text
realm_access.roles
```

GET:

```http
GET http://localhost:8081/enrollments
Authorization: Bearer <ADMIN_TOKEN>
```

Expected:

```text
200 OK
```

POST:

```http
POST http://localhost:8081/enrollments
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

Expected:

```text
2xx
```

depending on the endpoint response.


14. TEST WITHOUT TOKEN
    ======================

Call:

```http
GET http://localhost:8081/enrollments
```

without:

```text
Authorization: Bearer ...
```

Expected:

```text
401 Unauthorized
```

Typical response header:

```text
WWW-Authenticate: Bearer
```

Authentication has failed because no Bearer token was provided.


15. 401 VS 403
    ==============

Remember:

```text
401 Unauthorized
=
not authenticated
```

Examples:

```text
no token
expired token
invalid signature
wrong issuer
wrong audience
malformed token
```

While:

```text
403 Forbidden
=
authenticated, but not authorized
```

Example:

```text
ruben
+
valid JWT
+
ROLE_USER
+
POST endpoint requires ROLE_ADMIN
=
403 Forbidden
```


16. TOKEN EXPIRATION
    ====================

Access tokens expire.

If a token that previously worked starts returning:

```text
401
```

first check:

```text
exp
```

and obtain a fresh token.

In Postman:

```text
Authorization
→ OAuth 2.0
→ Get New Access Token
→ login
→ Use Token
```


17. POSTMAN AUTHENTICATE-VIA-BROWSER TIMEOUT
    ============================================

If Postman's browser OAuth flow times out:

Check:

```text
Keycloak is running
http://localhost:8083 is reachable
```

Check callback URI in Keycloak:

```text
https://oauth.pstmn.io/v1/browser-callback
```

Check Postman uses exactly the same callback.

Also check that an old browser session is not logging in automatically as the wrong user.

To test another user:

```text
logout from Keycloak in the browser
```

or use:

```text
private/incognito browser window
```

Then request a fresh token again.


18. PORT 8081 ALREADY IN USE
    ============================

If Spring Boot fails with:

```text
Web server failed to start.
Port 8081 was already in use.
```

Find the process:

```powershell
netstat -ano | findstr :8081
```

Example:

```text
TCP    0.0.0.0:8081    0.0.0.0:0    LISTENING    12345
```

The last value is the PID.

Inspect it:

```powershell
Get-Process -Id 12345
```

Kill it:

```powershell
Stop-Process -Id 12345 -Force
```

or:

```powershell
taskkill /PID 12345 /F
```

Then restart the Enrollment Service.


19. CURRENT VERIFIED RBAC BEHAVIOR
    ==================================

The security flow has been tested successfully.

Verified behavior:

```text
No token
    ↓
401 Unauthorized

ruben + USER role
    ↓
GET /enrollments
    ↓
200 OK

ruben + USER role
    ↓
ADMIN-only operation
    ↓
403 Forbidden

admin + ADMIN role
    ↓
protected ADMIN operation
    ↓
allowed
```

This confirms both:

```text
JWT authentication
```

and:

```text
role-based authorization (RBAC)
```

are working as intended.


20. COMPLETE SECURITY FLOW
    ==========================

```text
                         ┌──────────────────────────┐
                         │        Keycloak          │
                         │  localhost:8083          │
                         └────────────┬─────────────┘
                                      │
                             login + authorization
                                      │
                                      ▼
                         ┌──────────────────────────┐
                         │        Postman           │
                         │ Authorization Code + PKCE│
                         └────────────┬─────────────┘
                                      │
                                Access Token
                                      │
                                      ▼
                    Authorization: Bearer <JWT>
                                      │
                                      ▼
                  ┌──────────────────────────────────┐
                  │       Enrollment Service         │
                  │          port 8081               │
                  └────────────────┬─────────────────┘
                                   │
                           Spring Security
                                   │
                   ┌───────────────┴───────────────┐
                   │                               │
                   ▼                               ▼
             Validate JWT                    Map roles
                   │                               │
          signature / issuer /               realm_access
          expiration / audience                   │
                   │                               ▼
                   │                      ROLE_USER / ROLE_ADMIN
                   │                               │
                   └───────────────┬───────────────┘
                                   │
                                   ▼
                          Authorization rules
                                   │
                     ┌─────────────┴─────────────┐
                     │                           │
                  allowed                     denied
                     │                           │
                     ▼                           ▼
                 Controller                    403
```


21. MENTAL MODEL
    ================

Keycloak is the:

```text
Authorization Server / Identity Provider
```

Postman is the:

```text
OAuth2 Client
```

Enrollment Service is the:

```text
OAuth2 Resource Server
```

The access token proves:

```text
who authenticated
```

and contains information used to determine:

```text
what the caller may access
```

The flow is:

```text
User
  ↓
Keycloak authenticates user
  ↓
Keycloak issues JWT
  ↓
Postman sends JWT
  ↓
Spring validates JWT
  ↓
Spring converts Keycloak roles
  ↓
Spring evaluates RBAC rules
  ↓
Controller executes
```
