# Kakao OIDC app exchange-code flow review

Source of truth:
- `.omx/plans/kakao-oidc-app-exchange-code-20260419T060053Z.md`
- `_auth_template/Auth-Template` as reference concept only

## Why this document exists
This note captures the implementation review and frozen integration contract for the approved Kakao mobile app login redesign. It is intended to help the implementation and test lanes converge on the same behavior before merge.

## Approved end-to-end flow
1. App opens `GET /api/v1/auth/kakao/login` in a browser/custom tab.
2. Request includes `channel`, `target`, `codeChallenge`, and `codeChallengeMethod`.
3. Backend validates the target allowlist and proof policy, stores app login context in the session-backed OAuth authorization request flow, and redirects to `/oauth2/authorization/kakao`.
4. Spring Security completes the Kakao OIDC callback with its own `state`/`nonce` handling.
5. Backend validates the OIDC identity (`OidcUser`, issuer, subject), upserts the Polaris user, creates a short-lived one-time exchange record, and redirects only the opaque exchange code to the allowlisted app target.
6. App calls `POST /api/v1/auth/exchange` with `code`, `targetId`, and `codeVerifier`.
7. Backend validates `code + targetId + verifier`, issues Polaris access/refresh tokens once, and returns the existing `AuthTokenResponse` shape.

## Frozen contract that should not drift during implementation
### Login start request
`GET /api/v1/auth/kakao/login`
- `channel`
- `target`
- `codeChallenge`
- `codeChallengeMethod`

Rules:
- app mode requires `codeChallenge` by default
- `codeChallengeMethod` must be exactly `S256` whenever `codeChallenge` is present
- no arbitrary callback URL input; backend resolves `target` through an allowlist

### Exchange request
`POST /api/v1/auth/exchange`

```json
{
  "code": "opaque one-time code",
  "targetId": "kakao-app",
  "codeVerifier": "43-128 chars, RFC7636 charset"
}
```

Rules:
- exchange is `permitAll`
- JWT filter must skip `/api/v1/auth/exchange`
- accidental bearer headers must not break exchange
- proof-bound rows require `codeVerifier`
- legacy proofless exchange remains temporary and default-off

### Success/failure callback behavior
- App success: `303` redirect with opaque exchange code only
- App failure: `303` redirect with safe error slug only
- Non-app success: legacy JSON callback allowed only behind `core.auth.oidc.legacy-json-callback.enabled=false` by default
- Non-app disabled path: `403 application/problem+json` with `errorCode=OIDC_NON_APP_CALLBACK_DISABLED`

## Current Polaris baseline review
### Already aligned
- Kakao login already uses Spring OAuth2 client configuration with `openid` scope and `issuer-uri`
- `KakaoOidcUserService` already uses `OidcUser` and upserts by `iss/sub`
- refresh/logout token lifecycle already exists and can be reused by exchange redemption

### Gaps against the approved plan
1. **Login entrypoint too simple**
   - `AuthController.redirectToKakaoLogin()` only redirects to `/oauth2/authorization/kakao`
   - no channel/target/proof input handling
   - no allowlisted target resolution
   - no app login context propagation
2. **Success handler still returns tokens immediately**
   - `OAuth2AuthenticationSuccessHandler` writes `OidcLoginResponse` JSON directly
   - approved app flow requires exchange-code issuance and redirect instead of immediate token delivery
3. **Failure handler is not app-aware**
   - current failure path always returns `401 problem+json`
   - approved app flow requires safe redirect error propagation for app mode and explicit ProblemDetail contracts for non-app mode
4. **Security chain incomplete for exchange**
   - `SecurityConfig` does not permit `/api/v1/auth/exchange`
   - `JwtAuthenticationFilter.shouldNotFilter()` does not exempt `/api/v1/auth/exchange`
5. **No exchange-code domain model yet**
   - no exchange entity/repository/service
   - no one-time terminal state semantics
   - no proof-binding storage
6. **ProblemDetail style is incomplete for the approved auth errors**
   - current handlers typically set `title` and `detail` only
   - approved auth flow needs explicit `errorCode` values on ProblemDetail responses
7. **Configuration gap**
   - `application.yaml` has Kakao OIDC client settings and JWT settings only
   - approved flow still needs app-target allowlist, TTL, proof-required flag, legacy JSON flag, and proofless fallback flag
8. **Test gap**
   - existing tests cover refresh/JWT/security handlers only
   - no tests yet for login context propagation, exchange binding, duplicate redemption, callback branches, or compatibility flags

## Required integration deltas by area
### Security and routing
- `SecurityConfig.kt`
  - add `permitAll` for `/api/v1/auth/exchange`
  - keep OAuth callback/login/refresh accessible
  - wire a session-aware authorization-request repository decorator
- `JwtAuthenticationFilter.kt`
  - skip `/api/v1/auth/exchange`

### Auth web layer
- `AuthController.kt`
  - expand login endpoint contract to accept `channel`, `target`, `codeChallenge`, `codeChallengeMethod`
  - add `POST /api/v1/auth/exchange`
- `OAuth2AuthenticationSuccessHandler.kt`
  - split app vs non-app behavior
  - app mode must redirect with opaque exchange code only
- `OAuth2AuthenticationFailureHandler.kt`
  - split app redirect failure vs non-app ProblemDetail failure

### Auth domain layer
- add app login context model + resolver/service
- add session-backed authorization-request repository decorator
- add exchange-code entity/repository/service with terminal consume semantics
- reuse existing token issuance/refresh/logout behavior from `AuthTokenService`

### Error handling
Add explicit auth exchange error codes at minimum for:
- `OIDC_PROOF_REQUIRED`
- `OIDC_UNSUPPORTED_CODE_CHALLENGE_METHOD`
- `OIDC_INVALID_CODE_VERIFIER`
- `OIDC_PROOF_MISMATCH`
- `OIDC_NON_APP_CALLBACK_DISABLED`
- `OIDC_INVALID_TARGET`
- `OIDC_EXCHANGE_CODE_EXPIRED`
- `OIDC_EXCHANGE_CODE_ALREADY_CONSUMED`
- `OIDC_EXCHANGE_CODE_NOT_FOUND`
- `OIDC_EXCHANGE_ISSUANCE_FAILED`

## Merge-review checklist
Use this checklist before integrating implementation branches:
- [ ] `GET /api/v1/auth/kakao/login` rejects unknown target IDs and arbitrary redirect URLs
- [ ] app mode requires `codeChallenge` unless compatibility flag is explicitly enabled
- [ ] `codeChallengeMethod=S256` is enforced exactly
- [ ] Spring-managed `state` and `nonce` handling remain intact
- [ ] app success never places Polaris access/refresh tokens in redirect URLs
- [ ] exchange rows are single-use and terminal under duplicate redemption
- [ ] `/api/v1/auth/exchange` is `permitAll`
- [ ] JWT filter skips `/api/v1/auth/exchange`
- [ ] ProblemDetail auth failures include `errorCode`
- [ ] non-app JSON callback is default-off and clearly marked temporary
- [ ] proofless exchange fallback is default-off and clearly marked temporary
- [ ] refresh/logout flows still work after token issuance moves behind exchange redemption

## Verification checklist expected from the implementation lane
### Required automated checks
- callback branch tests for app success, app failure, non-app disabled, non-app legacy compatibility
- exchange tests for valid proof-bound redemption, wrong target, missing verifier, mismatched verifier, already-consumed code, and expired code
- security tests proving `/api/v1/auth/exchange` is permit-all and JWT-filter-exempt
- refresh/logout regression tests after exchange-based issuance

### Required manual/e2e checks
- browser/custom-tab login reaches Kakao and returns to the app allowlisted target
- app receives an opaque exchange code, not Polaris tokens
- app redeems the exchange code once and receives access/refresh tokens
- second redemption of the same code fails without issuing another token pair

## Recommended implementation guardrails
- keep legacy JSON callback and proofless exchange behind explicit flags and document removal conditions
- hash stored exchange codes and code challenges; never log the raw verifier
- keep the exchange flow inside the auth feature slice rather than moving behavior into `global/config`
- prefer one new bounded abstraction per concern (login context, exchange service, exchange repository) over broad new layers
