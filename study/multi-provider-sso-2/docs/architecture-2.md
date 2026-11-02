# Architecture 2: Single Gateway with User-Selected IdP Choice

## Summary
The frontend asks employee or partner? and the gateway passes `kc_idp_hint`, so Keycloak goes straight to the right login.

## Authentication Flow
1. Browser loads `/architecture-2/`, calls `GET /api/architecture-2/me`, receives 401.
2. Frontend renders ChooseLogin UI with two buttons: "Sign in as Employee" and "Sign in as Partner".
3. User clicks "Sign in as Employee" -> navigates to `/api/architecture-2/login/internal`.
4. Gateway (`gw-2`) intercepts `/login/internal` and appends `kc_idp_hint=internal-kc1` to the OIDC authorization request to `keycloak-2`.
5. `keycloak-2` immediately forwards the browser to `keycloak-1`, skipping its own login form.
6. `keycloak-1` authenticates the user (password locally, SPNEGO/Kerberos on AD domain).
7. User is redirected back through `keycloak-2` broker and `gw-2` callback.
8. `gw-2` sets `X-RemoteUser`, `X-RemoteUserRole`, and `X-RemoteUserIdp=internal-kc1`, then proxies `/login` to backend.
9. Backend validates `gw-2` credentials, mints `architecture-2-session` JWT cookie (`aud: architecture-2:any`), redirects to `/architecture-2/`.
10. If user clicks "Sign in as Partner" -> navigates to `/api/architecture-2/login/external` (no hint); `keycloak-2` authenticates directly.

## Trade-offs & Review Notes

### Pros
- Employees get a direct one-click path to `keycloak-1` (zero-click on domain PCs).
- Straightforward implementation with `mod_auth_openidc` path-based parameter injection.
- Does not depend on email domain parsing for employee routing.

### Cons
- Requires user to choose the correct button.
- If an off-network employee clicks Employee without line-of-sight to `keycloak-1`, connection fails.
- The choice is a UI routing mechanism, not a network security boundary.

### Implementation Notes
- Adding external users directly requires a `keycloak-2` service account.
- Enables adding additional external identity providers on `keycloak-2`.
- Password reset and registration are handled via Keycloak.
- The partner path still shows `keycloak-2` email page because Organizations feature is enabled on the external realm.

## Try This
- Click "Sign in as Employee" -> observe immediate redirect to `keycloak-1` without `keycloak-2` email prompt.
- On `keycloak-1`, try logging in with `carol@partner.example.com` (fails: carol exists only in `keycloak-2`).
- Click "Sign in as Partner" -> authenticate as carol on `keycloak-2`.
