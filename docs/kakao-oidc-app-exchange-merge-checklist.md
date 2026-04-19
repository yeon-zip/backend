# Kakao OIDC app exchange-code merge checklist

Use this checklist when integrating the implementation and test branches for the approved mobile app exchange-code flow.

## API contract checks
- [ ] `GET /api/v1/auth/kakao/login` accepts `channel`, `target`, `codeChallenge`, `codeChallengeMethod`
- [ ] `POST /api/v1/auth/exchange` accepts JSON `code`, `targetId`, `codeVerifier`
- [ ] app login rejects unknown `target` values before redirecting to Kakao
- [ ] `codeChallengeMethod` accepts only `S256`
- [ ] default path requires proof binding for app logins
- [ ] compatibility flags are default-off

## Callback behavior checks
- [ ] app success uses `303` redirect to an allowlisted target
- [ ] app success includes only an opaque exchange code
- [ ] app failure uses `303` redirect with a safe error slug only
- [ ] non-app callback stays disabled unless legacy compatibility flag is enabled
- [ ] non-app disabled path returns `403 application/problem+json`

## Security checks
- [ ] `SecurityConfig` explicitly marks `/api/v1/auth/exchange` as `permitAll`
- [ ] `JwtAuthenticationFilter.shouldNotFilter()` skips `/api/v1/auth/exchange`
- [ ] accidental bearer headers on the exchange route do not cause JWT rejection
- [ ] Spring Security remains responsible for `state` and `nonce`

## Persistence + token checks
- [ ] exchange codes are stored hashed
- [ ] proof-bound exchanges store challenge material without persisting raw verifier values
- [ ] first valid redemption issues tokens exactly once
- [ ] second redemption returns a terminal error and never issues another refresh token
- [ ] refresh/logout behavior still works with exchange-issued tokens

## Error-contract checks
- [ ] new auth ProblemDetail responses include `errorCode`
- [ ] proof-required failures use `OIDC_PROOF_REQUIRED`
- [ ] verifier syntax failures use `OIDC_INVALID_CODE_VERIFIER`
- [ ] verifier mismatch failures use `OIDC_PROOF_MISMATCH`
- [ ] disabled non-app callbacks use `OIDC_NON_APP_CALLBACK_DISABLED`

## Test evidence to collect before merge
- [ ] callback success/failure branch tests
- [ ] exchange redemption success/failure tests
- [ ] duplicate redemption race/idempotency test
- [ ] security-chain tests for permit-all + JWT-filter skip
- [ ] refresh/logout regression tests
