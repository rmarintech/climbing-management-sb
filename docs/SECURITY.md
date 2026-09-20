# Spring Security — Enrollment Service

Learning notes for HTTP Basic, role-based access control, CSRF and stateless authentication in the Climbing Management project.

[Back to README](../README.md) · [Progress and next steps](ROADMAP.md)

Progress checkboxes belong only in ROADMAP.md. This guide records the theory and exercises covered so far; it is not a claim that every example has been implemented or verified.

## 1. Security at the application boundary

Spring Security protects incoming HTTP requests before they reach the REST controller. Domain objects and application use cases remain independent of HTTP authentication configuration.

Authentication establishes the caller's identity. Authorization determines whether that identity may perform an operation. CSRF protection is an additional request check; having the ADMIN role does not bypass it.

| Concern | Question | Enrollment example |
| --- | --- | --- |
| Authentication | Who is calling? | Validate Basic credentials for `ruben` or `admin` |
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

Add this to the existing HttpSecurity chain before `build()`:

```java
.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

```java
import org.springframework.security.config.http.SessionCreationPolicy;
```

Spring Security then does not create or use an HTTP session to persist authentication. This is not a global prohibition on other application code creating a session. It also does not mean the application has no database, cache or business state.

| Policy | Authentication session behavior |
| --- | --- |
| `IF_REQUIRED` | Create a session when needed |
| `NEVER` | Do not create one, but an existing session can be used |
| `STATELESS` | Do not create or use one to retain the security context |

Successful authentication on request A does not authenticate request B. Request B must supply its own credentials.

This configuration has been introduced as the current exercise. Verification is still pending; see ROADMAP.md.

### Verification sequence

Use a non-browser API client. For these checks, restore the exercise's disabled CSRF configuration and restart the application.

| Step | Request | Authentication | Expected |
| --- | --- | --- | --- |
| 1 | `GET /enrollments` | Admin Basic credentials | `200` |
| 2 | `GET /enrollments` | None | `401` |
| 3 | `GET /enrollments` | Ruben Basic credentials | `200` |
| 4 | `POST /enrollments` | Ruben Basic credentials | `403` |
| 5 | `POST /enrollments` | Admin Basic credentials | `201` |
| 6 | `POST /enrollments` | None | `401` |

For POST success, use a valid new enrollment and an existing course with the required dependencies available. Business validation and dependency failures can otherwise produce different responses.

For unauthenticated checks, select No Auth and remove manually configured or inherited Authorization headers. Browser credential caching can hide this distinction.

These behavioral checks may have worked before the explicit setting because HTTP Basic can already operate statelessly. The configuration makes the intended policy explicit; status codes alone are not proof of the internal session setup.

## 7. Security and the existing OpenAPI contract

The previously documented business responses remain relevant: GET `200`; POST `201`, `400`, `404` and `503`. Security introduces additional `401` and `403` outcomes.

Aligning the OpenAPI security scheme, operation requirements and those responses is upcoming work. Do not assume that Spring Security error bodies use the application's generated ErrorResponse: filter-level failures need their own response handling if a consistent JSON contract is required.

## 8. Interview questions

**What is the difference between authentication and authorization?**

Authentication identifies a caller; authorization decides which actions that caller may perform.

**Can an authenticated user receive 403?**

Yes. The user may lack the required authority, or another protection such as CSRF may reject the request.

**Does ADMIN automatically include USER?**

No. Allow both explicitly or configure a deliberate role hierarchy.

**Is HTTP Basic encrypted?**

No. Its header encodes credentials with Base64. TLS supplies transport encryption.

**Does STATELESS mean no persistence?**

No. It concerns authentication session persistence. Business data may still be persisted normally.

**Can a stateless application be vulnerable to CSRF?**

Yes, when browsers automatically attach authentication credentials. Evaluate credential transport, not just whether a server session exists.

**Why keep security configuration outside the domain model?**

HTTP authentication is an infrastructure boundary concern. The domain retains business invariants independently of Spring Security. Business-specific ownership rules may still require application/domain-level enforcement as the system evolves.

## 9. Next topic and commits

After verifying stateless behavior, proceed to OAuth2, OpenID Connect and JWT. These are upcoming topics, not completed capabilities.

Suggested code checkpoint after successful verification:

```bash
git commit -m "feat(security): configure stateless HTTP Basic and enrollment RBAC"
```

Documentation checkpoint from the repository root:

```bash
git add README.md docs/ROADMAP.md docs/SECURITY.md
git diff --cached
git commit -m "docs(security): document HTTP Basic RBAC CSRF and stateless exercise"
```

## References

- [Spring Security: CSRF concepts and browser credentials](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html)
- [Spring Security: servlet CSRF configuration](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security: authentication persistence and stateless sessions](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
