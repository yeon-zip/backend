# Auth Review: Polaris backend vs `_auth_template/Auth-Template`

## Summary
- Polaris currently has only the global Spring Security shell (`global/config/SecurityConfig.kt` plus custom handlers).
- The auth template contains a complete auth slice: OAuth2 login handlers, JWT token issuance/refresh/logout, refresh-token persistence, and a user domain.
- For Polaris, the template should be treated as a reference architecture, not copied verbatim. The current Polaris codebase uses package-by-feature with `presentation/service/implement/repository/entity`, so auth should follow the same layering.

## Current Polaris baseline
### Present today
- `global/config/SecurityConfig.kt`
  - stateless session policy
  - custom access denied / authentication entrypoint handlers
  - `anonymous` disabled
  - `/admin/**` requires authentication, everything else is `permitAll`
- No auth package, no user package, no JWT utilities, no OAuth2 client config, no user entity/repository/service, and no authenticated `me` endpoint.

### Gap vs template
The template adds four capabilities Polaris does not currently have:
1. OAuth2/OIDC client integration
2. JWT access/refresh token lifecycle
3. persisted refresh-token invalidation/logout
4. user domain + authenticated API surface

## Template structure worth reusing conceptually
### Template packages
- `security/oauth2/*`
  - provider mapping, OAuth2/OIDC user loading, success/failure handlers
- `security/jwt/*`
  - token provider, auth service, filter, refresh-token entity/repository, auth controller
- `user/*`
  - user entity/repository/service/controller

### Polaris-style target mapping
To stay aligned with the existing Polaris layering, the equivalent slices should be organized more like:
- `auth/presentation/*` — login, refresh, logout endpoints
- `auth/service/*` — orchestration/use-case layer
- `auth/implement/*` — JWT helpers, OIDC user loading, token persistence helpers
- `auth/repository/*` — refresh token persistence
- `auth/entity/*` — refresh token entity
- `user/entity|repository|service|presentation/*` — user aggregate and `me` API
- `global/config/*` — only shared security wiring and properties beans

This keeps security/auth concerns feature-scoped instead of concentrating all auth logic under `global`.

## Review findings on the template
### Good reference points
- Clear separation between OAuth login handling and JWT refresh lifecycle
- Refresh-token persistence enables logout / invalidation instead of purely stateless refresh tokens
- Success/failure handlers make the login flow explicit
- `AuthService` centralizes refresh rotation and logout rules

### Risks / quality issues to avoid copying directly
- Template uses generic OAuth2 user parsing; the task requires Kakao **OIDC**, so Polaris should prefer `OidcUser` + issuer-based validation instead of raw provider-specific attribute parsing.
- Template success flow redirects with a temporary access token in query parameters. That can be acceptable as a transitional pattern, but it is sensitive and should stay minimal and explicitly documented if retained.
- Template security config currently permits almost everything (`anyRequest().permitAll()`), which is too loose as a long-term Polaris baseline.
- Template is organized partly by technical layer (`security/jwt`, `security/oauth2`) rather than Polaris feature-first structure.
- Template test coverage is minimal relative to the amount of auth behavior it implements.

## Minimal Kakao OIDC port for Polaris
### Configuration requirements
Polaris will need Spring OAuth2 client configuration for Kakao OIDC with:
- `issuer-uri`
- `client-id`
- `client-secret`
- `redirect-uri`
- `scope` including `openid`
- nonce-aware login flow handled by Spring Security OIDC support

### Recommended minimum behavior
1. Kakao login via OIDC (`OidcUser`, not plain `OAuth2User`)
2. user upsert on successful OIDC login
3. JWT access/refresh token issuance
4. refresh token rotation
5. logout/token invalidation endpoint
6. authenticated `me` endpoint backed by the Polaris user domain
7. security rules updated so authenticated APIs are no longer globally `permitAll`

## Suggested endpoint surface for Polaris
To match existing controller naming, a minimal surface would be:
- `POST /api/auth/refresh`
- `POST /api/auth/logout` or `DELETE /api/auth/logout`
- `GET /api/users/me`

If worker-1/worker-2 implement code from the template, these endpoints should be reviewed against existing Polaris naming before merge.

## Integration notes for the implementation lane
- Add only the auth-specific Spring dependencies actually required for OIDC/JWT.
- Prefer Kakao OIDC discovery via `issuer-uri` over hand-written provider endpoints when possible.
- Keep JWT parsing/issuance out of `global/config`; wiring belongs there, behavior does not.
- Persist refresh tokens server-side so logout and rotation semantics remain enforceable.
- Add focused tests around: login success, refresh rotation, invalid refresh rejection, logout invalidation, and `me` authentication.

## Reviewer recommendation
The implementation lane should be considered a **new auth feature slice**, not a small edit to the existing security config. The template is a useful reference for flow shape, but Polaris should only port the minimal Kakao OIDC + JWT refresh pieces needed and keep the final package layout consistent with existing feature modules.
