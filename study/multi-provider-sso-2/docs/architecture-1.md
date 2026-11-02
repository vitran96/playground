# Architecture 1: Single Gateway with Email-First Identity Routing

## Summary
One URL for everyone. keycloak-2 asks for an email first; company emails go to keycloak-1, everyone else types a password.

## Authentication Flow
1. Browser loads `/architecture-1/`, calls `GET /api/architecture-1/me`, receives 401.
2. Browser navigates to `GET /api/architecture-1/login`. Gateway (`gw-1`) checks session, finds none, and redirects (302) with PKCE to `keycloak-2`.
3. `keycloak-2` displays its identity-first prompt: "Email?".
4. User enters company email (`@internal.email.com`); `keycloak-2` Organization matches domain and redirects (302) to `internal-kc1` IdP (`keycloak-1`).
5. `keycloak-1` authenticates the user (password locally, SPNEGO/Kerberos in domain environment).
6. `keycloak-1` returns authorization code to `keycloak-2` broker endpoint.
7. `keycloak-2` exchanges code with `keycloak-1` backchannel, mints its own token, and returns code to `gw-1` redirect URI.
8. `gw-1` completes code exchange, validates ID token, strips incoming headers, sets `X-RemoteUser`, `X-RemoteUserRole`, and `X-RemoteUserIdp`, then proxies `/login` to backend.
9. Backend verifies `gw-1` secret, sets HttpOnly JWT session cookie (`architecture-1-session`), and redirects (302) to `/architecture-1/`.
10. If user entered an external email (e.g. `carol@partner.example.com`), `keycloak-2` prompts for password directly without contacting `keycloak-1`.

## Trade-offs & Review Notes

### Pros
- Single entry URL and single OIDC client configuration.
- Internal users enter only their email address once per session.
- No network-level routing or split DNS required; functions identically at home, office, and Zscaler.

### Cons
- Not zero-click on initial visit (requires email entry).
- Depends on Keycloak Organizations feature (configuration schema is version-sensitive).
- Employee browsers must have network reachability to `keycloak-1`.

### Implementation Notes
- Adding external users directly from the app requires a `keycloak-2` service account (confidential client with `realm-management` -> `manage-users`).
- Enables adding additional external OIDC identity providers later (e.g. Entra ID, Google) on `keycloak-2` with zero app or gateway changes.
- Self-service password reset and user registration are managed by Keycloak (emails captured in Mailpit).
- Future optimization: App could store an "employee" cookie preference and send `kc_idp_hint` to bypass the email step on return visits.

## Try This
- Log in as `alice@internal.email.com` (password: `alice`): observe redirect to `keycloak-1`, session shows `user_type: internal`, roles: `employee, app-admin`.
- Log in as `carol@partner.example.com` (password: `carol`): stays on `keycloak-2`, session shows `user_type: external`, role: `partner`.
- Verify that session card displays audience `architecture-1:any`.
