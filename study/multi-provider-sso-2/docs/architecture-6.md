# Architecture 6: Two Domains, Keycloak Login with App-Owned Registration

## Summary
Architecture 3 with an app-owned front door on the external domain. The app shows its own login page (Sign In / Register). Sign-in still happens in `keycloak-2`; registration is done by the app, which creates the user in `keycloak-2` and emails a temporary password.

## Authentication Flow
### Internal Flow (`http://int.localhost:8601`)
Unchanged from Architecture 3: `gw-6-int` goes straight to `keycloak-1`; cookie `architecture-6-internal-session`, audience `architecture-6:internal`.

### External Flow (`http://ext.localhost:8602`)
1. The app does not redirect immediately; `loginUi="choice"` renders the app login page with **Sign In** and **Register**.
2. Sign In: `gw-6-ext` protects `/api/architecture-6/login`, OIDC against `keycloak-2` (client `app-arch6-ext`). Forgot password is `keycloak-2`'s own email-OTP reset.
3. Register: `POST /api/architecture-6/self/register` (first name, last name, email). Backend creates the user in `keycloak-2` via the `backend-admin` service account with a random **temporary** password and role `partner`, and emails it via Mailpit.
4. At first sign-in `keycloak-2` forces a new password.
5. Backend issues cookie `architecture-6-external-session` with audience `architecture-6:external`.

## Implementation Notes
- Gateways reuse `gw-3-int.conf` / `gw-3-ext.conf` (parameterized by `ARCH`).
- The `keycloak-2` Register link is hidden for `app-arch6-ext` via the `arch6-external` login theme. The realm still allows self-registration (shared with Architecture 3); use a dedicated realm with registration disabled in production.
- See [architecture-3.md](architecture-3.md) for the shared two-domain trade-offs.

## Try This
- Register on `ext.localhost:8602/architecture-6/`, read the temporary password in Mailpit (`http://localhost:8125`), Sign In, set a new password.
