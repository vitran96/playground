# Architecture 2: Direct Multiple Identity Providers Pattern

## 1. Overview & Concept
Architecture 2 implements the **Direct Multi-Provider** pattern. Instead of relying on an intermediary identity broker, the application backend connects directly to multiple independent OIDC providers (`kc-internal` and `kc-external`).

The application UI exposes separate, explicit login actions for internal staff and external partners. The classification of the user is established by **which callback route completed the flow**, cryptographically verified against the corresponding provider's token issuer (`iss`).

```
                              ┌──(1a. Sign in Internal)──> [ kc-internal:8081 ] ──(Callback /auth/a2/internal/callback)──┐
                              │                                                                                            │
[ Browser ] ──> [ App:3000 ] ─┤                                                                                            ├──> [ App Session established ]
                              │                                                                                            │
                              └──(1b. Sign in External)──> [ kc-external:8082 ] ──(Callback /auth/a2/external/callback)──┘
```

---

## 2. Real-World Context & Trade-Offs

### When to Choose This Pattern
- Lean organizations or microservices architectures where maintaining an intermediate Keycloak broker cluster is unnecessary overhead.
- Systems connecting to completely distinct, non-federated identity systems (e.g., Corporate Google Workspace / Okta for staff vs. Auth0 / Azure AD B2C for customers).
- User workflows where staff and external users access through distinct portals or entry buttons.

### Advantages
- **No Broker Overhead**: Eliminates one complete Keycloak instance, reducing infrastructure cost, latency, and operational points of failure.
- **Explicit User Control**: The user chooses their login path upfront without reaching an intermediate broker selection page.
- **Provider-Specific Scopes**: Each provider connection can negotiate completely independent OAuth scopes, client secrets, and backchannel token endpoints.

### Disadvantages & Risks
- **Multiple Token Contracts**: The application backend must maintain multiple OIDC client IDs, secrets, discovery caches, and JWKS sets.
- **Route / Issuer Mismatch Vulnerabilities**: The backend must strictly ensure that a token issued by Provider A cannot be exchanged or accepted on the callback route for Provider B.
- **Linear Code Growth**: Supporting an Nth provider requires adding another callback route and provider configuration in the app.

---

## 3. Topology & Configuration

| Component | Endpoint / Port | Keycloak Realm / Client | Purpose |
|-----------|-----------------|-------------------------|---------|
| **Demo App** | `http://localhost:3000` | Client: `demo-app-a2` | App backend & UI |
| **Corp Intranet** | `http://localhost:8081` | Realm: `internal` | Direct IdP for staff (`alice`, `bob`) |
| **Partner Portal** | `http://localhost:8082` | Realm: `external` | Direct IdP for partners (`xavier`, `yen`) |

### Provider Configurations (`app/src/config.js`)
- `a2-internal`:
  - Issuer: `http://localhost:8081/realms/internal`
  - Backchannel: `http://kc-internal:8080/realms/internal`
  - Redirect URI: `http://localhost:3000/auth/a2/internal/callback`
  - Client ID: `demo-app-a2`, Secret: `a2-internal-secret`
- `a2-external`:
  - Issuer: `http://localhost:8082/realms/external`
  - Backchannel: `http://kc-external:8080/realms/external`
  - Redirect URI: `http://localhost:3000/auth/a2/external/callback`
  - Client ID: `demo-app-a2`, Secret: `a2-external-secret`

---

## 4. End-to-End Authentication Flow

1. **Initiation**: User clicks **Sign in as internal** or **Sign in as external** at `/architecture-2`.
   - Internal clicks trigger `GET /auth/a2/internal/login`.
   - External clicks trigger `GET /auth/a2/external/login`.
2. **Provider Redirection**:
   - Internal redirects to `http://localhost:8081/realms/internal/protocol/openid-connect/auth`.
   - External redirects to `http://localhost:8082/realms/external/protocol/openid-connect/auth`.
3. **Authentication**: User logs in directly at the realm (green theme for internal, orange theme for external).
4. **Provider Callback**:
   - Internal returns to `GET /auth/a2/internal/callback?code=...&state=...`.
   - External returns to `GET /auth/a2/external/callback?code=...&state=...`.
5. **Code Exchange & Validation**:
   - `finishLogin()` verifies `pending.state` for that specific provider key.
   - Code exchange occurs with the matching provider's backchannel token endpoint.
   - ID token signature is verified against that provider's JWKS.
6. **Classification**:
   - Callback on `/auth/a2/internal/callback` yields `userType = 'internal'`.
   - Callback on `/auth/a2/external/callback` yields `userType = 'external'`.
   - Reason recorded: `completed /auth/a2/{providerSide}/callback; iss={claims.iss}`.

---

## 5. Security & Threat Modeling

### Cross-Route Injection Prevention
What if an external user starts login on `/external`, receives an authorization code, but attempts to submit the code to `/internal/callback`?
- **State Check**: `finishLogin(c, config.providers['a2-internal'])` inspects `session.pending['a2-internal']`. Because the pending session was keyed to `a2-external`, the check fails with:
  `HTTP 400: no login in progress for this provider`.
- **Issuer Validation**: `jwtVerify()` specifies `issuer: provider.issuer`. An ID token issued by `kc-external` will fail signature/issuer validation if submitted to `a2-internal`.

---

## 6. Verification Script

1. **Internal Sign In**:
   - Go to `http://localhost:3000/architecture-2` -> Click **Sign in as internal** (green button).
   - Enter `bob` / `password`.
   - Verify Identity Card: `Decided by: login route + issuer`, `Reason: completed /auth/a2/internal/callback; iss=http://localhost:8081/realms/internal`.
   - User has internal privileges; can add internal notes.
2. **External Sign In**:
   - Click **Sign out** -> Click **Sign in as external** (orange button).
   - Enter `yen` / `password`.
   - Verify Identity Card: `Decided by: login route + issuer`, `Reason: completed /auth/a2/external/callback; iss=http://localhost:8082/realms/external`.
   - User has external privileges; cannot add internal notes.
3. **Cross-Realm Protection**:
   - Click **Sign in as external**, enter `alice` / `password`.
   - Result: Authentication fails at Keycloak because user `alice` does not exist in `kc-external`.
