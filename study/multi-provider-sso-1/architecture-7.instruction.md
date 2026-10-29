# Architecture 7: Central Broker with Automated IdP Hint Pattern

## 1. Overview & Concept
Architecture 7 combines the best attributes of **Architecture 1** (centralized broker token contract & role governance) and **Architecture 6** (seamless, frictionless user redirection):

In Architecture 1, navigating to the central broker forces the user to manually select between "Staff" and "Partner" on Keycloak's login screen. In Architecture 7, the application leverages standard Keycloak / OIDC parameter **`kc_idp_hint`**:

- Requesting `/auth/a7/login?hint=internal` adds `kc_idp_hint=internal` to the authorization request to `kc-central`.
- Keycloak's central broker detects the hint, **completely bypasses the IdP picker**, and immediately redirects the browser to `kc-internal`.
- Once authenticated, the upstream identity returns to `kc-central`, which executes identity provider mappers, injects realm roles (`internal_user` / `external_resolver`), and issues the central ID token to the application.

```
[ Browser ] ──(1. Login with hint)──> [ App:3000 ]
                                            │
                                  (2. 302 to kc-central with kc_idp_hint=internal)
                                            │
                                            ▼
                                   [ Keycloak Central:8080 ]
                                            │
                              (Bypasses broker selection UI!)
                                            │
                                            ▼
                               [ Corp Intranet:8081 (Staff) ]
                                            │
                             (User logs in with alice/password)
                                            │
                                            ▼
                          [ Returns to kc-central for role mapping ]
                                            │
                                            ▼
                       [ Central Token with verified roles issued to App ]
```

---

## 2. Real-World Context & Trade-Offs

### When to Choose This Pattern
- Enterprise architectures that want a single central token issuer and centralized role governance, but cannot tolerate the poor UX of an intermediate provider picker page.
- Portals with known user intent (e.g. employee portal bookmark vs. vendor portal bookmark) that still need centralized identity federation.
- Organizations combining Traefik network detection with central Keycloak: Traefik's `X-Network-Zone` translates directly to `kc_idp_hint`.

### Advantages
- **Zero-Friction UX**: Users land immediately on their home realm's login page without an extra click.
- **Single Token Contract**: App backend only needs one client ID (`demo-app-a1`), one secret, and one issuer validation (`kc-central`).
- **Unified Role Mapping**: Role governance remains centralized in Keycloak; upstream IdPs do not need to coordinate application role schemas.
- **Auto-Detection Support**: The backend can infer the hint automatically from `X-Network-Zone` or query params.

### Disadvantages & Risks
- **Upstream IdP Hint Validation**: If an invalid `kc_idp_hint` alias is passed, Keycloak falls back to displaying the manual selection screen or an error.
- **Upstream Dependency**: Still requires `kc-central` to be online to broker upstream authentication.

---

## 3. Topology & Configuration

| Component | Port | Role | Client |
|-----------|------|------|--------|
| **Demo App** | `http://localhost:3000` | Application frontend & API | Uses `demo-app-a1` |
| **Central Broker** | `http://localhost:8080` | Broker Realm (`central`) | Configured with IdPs: `internal`, `external` |
| **Corp Intranet** | `http://localhost:8081` | Staff Realm (`internal`) | Target of `kc_idp_hint=internal` |
| **Partner Portal** | `http://localhost:8082` | Partner Realm (`external`) | Target of `kc_idp_hint=external` |

### Keycloak IdP Configuration (`keycloak/realms/central.json`)
The identity provider aliases in `central.json` are:
- Alias: `internal` -> points to `http://localhost:8081/realms/internal`
- Alias: `external` -> points to `http://localhost:8082/realms/external`

---

## 4. End-to-End Authentication Flow

1. **Initiation**: User clicks **Sign in with Internal Hint** or **Sign in with External Hint** at `/architecture-7`.
2. **Backend Login Handler**:
   In `app/src/arch_extra.js`:
   ```javascript
   archExtraRouter.get('/auth/a7/login', (c) => {
     const zoneHeader = c.req.header('x-network-zone');
     const hint = c.req.query('hint') || (zoneHeader === 'external' ? 'external' : 'internal');
     return startLogin(c, config.providers['a1'], {
       callbackPath: '/auth/a7/callback',
       kc_idp_hint: hint
     });
   });
   ```
3. **Authorization URL Construction**:
   In `app/src/oidc.js`:
   ```javascript
   if (options.kc_idp_hint) {
     authUrl.searchParams.set('kc_idp_hint', options.kc_idp_hint);
   }
   ```
4. **Keycloak Broker Bypass**: Keycloak Central reads `kc_idp_hint=internal`, skips the provider picker, and issues an immediate 302 redirect to `http://localhost:8081/realms/internal/protocol/openid-connect/auth`.
5. **Upstream Login**: User signs in on the green internal page (`alice` / `password`).
6. **Role Mapping & Token Issuance**: Keycloak Internal returns to Keycloak Central, which maps role `internal_user`, injects session note claim `idp: "internal"`, and issues the authorization code to `/auth/a7/callback`.
7. **Verification**: Backend exchanges code and verifies claims via `classifyA1(claims)`.
   Session records:
   - `decidedBy: central broker (roles claim) + kc_idp_hint`
   - `reason: roles contains internal_user; auto-selected IdP via kc_idp_hint=internal`

---

## 5. Security & Verification Script

1. **Verify Internal Hint Bypass**:
   - Navigate to `http://localhost:3000/architecture-7`.
   - Click **Sign in with Internal Hint**.
   - Notice you immediately land on the **green Corp Intranet login page** (the blue central broker picker was completely bypassed).
   - Sign in as `alice` / `password`.
   - Identity Card displays:
     - `Decided by: central broker (roles claim) + kc_idp_hint`
     - `Reason: roles contains internal_user; auto-selected IdP via kc_idp_hint=internal`
     - Verified claims table shows: `iss: http://localhost:8080/realms/central`, `roles: ["internal_user"]`, `idp: "internal"`.
2. **Verify External Hint Bypass**:
   - Sign out -> Click **Sign in with External Hint**.
   - Immediately lands on the **orange Partner Portal login page**.
   - Sign in as `xavier` / `password`.
   - Identity Card displays `userType: external`, `roles: ["external_resolver"]`, `idp: "external"`.
3. **Verify Gateway Auto-Hint**:
   - Open `http://localhost:8083/architecture-7` (Traefik Internal).
   - Click **Auto Hint Sign In** (`/auth/a7/login`).
   - Backend reads `X-Network-Zone: internal` and automatically passes `kc_idp_hint=internal`.
