# Architecture 4: Two Domains with App-Owned External Login

## Summary
Internal users get zero-click Keycloak login; external users never see Keycloak, the app manages their accounts.

## Authentication Flow
### Internal Flow (`http://int.localhost:8401`)
1. Identical to Architecture 3: `gw-4-int` proxies to `keycloak-1` for zero-click SPNEGO on domain PCs.
2. Sets cookie `architecture-4-internal-session` with audience `architecture-4:internal`.

### External Flow (`http://ext.localhost:8402`)
1. `gw-4-ext` has no OIDC module configured; functions as a pure reverse proxy.
2. Frontend detects `loginUi="form"` from `GET /api/architecture-4/gateway` and renders local auth tabs.
3. Sign-in: `POST /api/architecture-4/local/login` validates password against in-memory scrypt hashes.
4. Backend issues cookie `architecture-4-external-session` with audience `architecture-4:external`.
5. Forgot password: `POST /api/architecture-4/local/forgot-password` logs a 15-minute one-time reset link to backend logs.

## Trade-offs & Review Notes

### Pros
- Eliminates `keycloak-2`: one fewer IdP cluster to maintain and patch.
- Full control of the external login, registration, and onboarding UX.
- Internal users retain true zero-click login.

### Cons
- Critical security logic (password hashing, reset tokens, auth endpoints) shifts to the application.
- Two distinct identity paradigms to maintain.
- Retains all two-domain trade-offs (DNS complexity, disjoint bookmark URLs).

### Implementation Notes
- Sign-up no longer depends on Keycloak: the app owns the external user store completely.
- The engineering team owns external credential security: hashing, reset flows, lockout/rate limiting, MFA, and compliance.
- Adding federated external OIDC providers later requires building OIDC handling inside the app or re-introducing an external IdP.

## Try This
- Sign up a new external partner on `ext.localhost:8402/architecture-4/`.
- Use "Forgot password", observe the reset link in `docker compose logs backend`, reset password, and log in.
- Notice that attempting to hit `/api/architecture-4/local/login` from the internal gateway (`int.localhost:8401`) returns 403.
