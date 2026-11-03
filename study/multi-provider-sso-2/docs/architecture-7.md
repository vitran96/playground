# Architecture 7: Two Domains, Keycloak Login with App-Owned Registration & Reset

## Summary
[Architecture 6](architecture-6.md) plus an app-owned **Forgot password**: the app emails a 6-digit code (same as Architecture 4) and sets the new password in `keycloak-2` through the admin API. Sign-in still happens in `keycloak-2`.

## Authentication Flow
### Internal Flow (`http://int.localhost:8701`)
Unchanged from Architecture 3 (`gw-7-int` -> `keycloak-1`).

### External Flow (`http://ext.localhost:8702`)
1. App login page with **Sign In**, **Register**, **Forgot Password**.
2. Sign In: `gw-7-ext` OIDC against `keycloak-2` (client `app-arch7-ext`).
3. Register: same as Architecture 6.
4. `POST /api/architecture-7/self/forgot-password`: looks up the user in `keycloak-2`, emails a 6-digit code (10 min, 5 tries, 1 per minute). Same response whether or not the account exists.
5. `POST /api/architecture-7/self/reset-password`: verifies the code, then `PUT /admin/realms/external/users/{id}/reset-password`.

## Implementation Notes
- Reset codes are in backend memory (shared store with Architecture 4, key-prefixed). Multiple backend replicas need a shared store.
- The `keycloak-2` Register and Forgot-password links are hidden for `app-arch7-ext` via the `arch7-external` login theme.
- The `backend-admin` service account can reset any external password: treat its secret as high-value.

## Try This
- Forgot Password for `carol@partner.example.com` on `ext.localhost:8702/architecture-7/`, read the code in Mailpit, reset, Sign In.
