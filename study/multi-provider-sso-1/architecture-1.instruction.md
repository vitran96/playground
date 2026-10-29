# Architecture 1: Central Identity Broker Pattern

## 1. Overview & Concept
Architecture 1 implements the **Central Identity Broker** pattern. The application delegates all authentication to a single central OIDC Identity Provider (`kc-central`). This broker realm integrates upstream external identity providers (`kc-internal` for staff and `kc-external` for partners).

The application backend only manages one client configuration (`demo-app-a1`), one issuer (`http://localhost:8080/realms/central`), and one set of token signing keys. Classification of whether the user is an internal staff member or an external partner is performed by evaluating the `roles` claim issued by the broker.

```
[ Browser ] ──(1. Login)──> [ App:3000 ] ──(2. 302 OIDC)──> [ Keycloak Central:8080 ]
                                                                       │
                                                       (Presents IdP Selection)
                                                                       │
                       ┌───────────────────────────────────────────────┴───────────────────────────────────────────────┐
                       ▼                                                                                               ▼
           [ kc-internal:8081 ] (Staff)                                                                    [ kc-external:8082 ] (Partner)
                       │                                                                                               │
                       └───────────────────────────────────────────────┬───────────────────────────────────────────────┘
                                                                       ▼
                                            [ kc-central maps upstream identity to realm role ]
                                                                       │
[ Browser ] <──(4. Session)── [ App:3000 verifies token & roles ] <────┘
```

---

## 2. Real-World Context & Trade-Offs

### When to Choose This Pattern
- Large enterprises with a dedicated IAM team managing an enterprise Identity Broker (e.g., Keycloak, Okta, Azure AD B2C, Ping Identity).
- Applications that need to support multiple customer or partner IdPs without modifying application code for every new IdP.
- Organizations requiring centralized audit logs, single sign-out across all providers, and centralized MFA policies.

### Advantages
- **Single Token Contract**: Application backend only validates one issuer (`iss`) and one JWKS endpoint.
- **Normalized Claims**: Central broker mappers translate heterogeneous upstream attributes into standardized application roles (`internal_user`, `external_resolver`).
- **Zero App Changes for New IdPs**: Adding a third partner organization requires only Keycloak broker configuration, zero application code changes.

### Disadvantages & Risks
- **Broker Dependency**: Central broker is a critical single point of failure (SPOF).
- **User UX Friction**: Users must explicitly choose between "Staff" and "Partner" buttons on the broker login screen if no IdP hint is provided.
- **Account Linking Complexity**: Upstream identity collisions require carefully designed broker first-login flows.

---

## 3. Topology & Configuration

| Component | Endpoint / Port | Keycloak Realm / Client | Purpose |
|-----------|-----------------|-------------------------|---------|
| **Demo App** | `http://localhost:3000` | Client: `demo-app-a1` | Web application & backend API |
| **Central Broker** | `http://localhost:8080` | Realm: `central` | Identity broker, role mapper |
| **Corp Intranet** | `http://localhost:8081` | Realm: `internal` | Upstream IdP for staff (`alice`, `bob`) |
| **Partner Portal** | `http://localhost:8082` | Realm: `external` | Upstream IdP for partners (`xavier`, `yen`) |

### Keycloak Role Mapping Configuration (`keycloak/realms/central.json`)
The central realm configures identity provider mappers:
- `internal-gets-internal_user`: Hardcoded role mapper assigning `internal_user` role to sessions originating from the `internal` identity provider alias.
- `external-gets-external_resolver`: Hardcoded role mapper assigning `external_resolver` role to sessions originating from the `external` identity provider alias.

---

## 4. End-to-End Authentication Flow

1. **Initiation**: User clicks **Sign in** at `/architecture-1`.
   - Browser requests `GET /auth/a1/login`.
   - App backend generates PKCE `verifier`, computes `code_challenge` (S256), and generates `state` and `nonce`.
   - App redirects browser (302) to `http://localhost:8080/realms/central/protocol/openid-connect/auth`.
2. **Broker Discovery**: `kc-central` presents the login theme (`demo-central`) with exactly two provider buttons:
   - **Staff (intranet sign-in)** -> redirects to `http://localhost:8081/realms/internal`.
   - **Partner sign-in** -> redirects to `http://localhost:8082/realms/external`.
3. **Upstream Authentication**: User signs in at the selected realm (e.g., `alice` / `password` on internal).
4. **Upstream Callback**: Selected realm redirects back to `kc-central/broker/{alias}/endpoint`.
5. **Role Injection & Code Issuance**: `kc-central` executes IdP mappers, maps the session note `identity_provider` to claim `idp`, injects the mapped role into the `roles` token claim, and redirects back to App:
   `GET /auth/a1/callback?code=...&state=...`
6. **Code Exchange**: App backend calls `POST http://kc-central:8080/realms/central/protocol/openid-connect/token` via backchannel, validates `nonce`, and cryptographically verifies the ID token using `kc-central` JWKS.
7. **Classification**: `classifyA1(claims)` verifies:
   - If `roles` has `internal_user` -> `userType = 'internal'`
   - If `roles` has `external_resolver` -> `userType = 'external'`
   - If neither or both -> **Fail closed** (`?error=no_role`).

---

## 5. Security & Fail-Closed Model

- **No Role -> Reject**: If an authenticated user does not have an explicit application role mapped by the broker, the system aborts session creation and redirects to `/architecture-1?error=no_role`.
- **RBAC Enforcement**:
  - `GET /api/a1/tickets`: Internal users receive all tickets (T-1 to T-4) with internal and shared notes. External users receive only assigned tickets with shared comments.
  - `POST /api/a1/tickets/:id/internal-note`: Enforces `session.a1.userType === 'internal'`. External users receive HTTP 403 Forbidden.

---

## 6. Verification Script

1. **Internal Path**:
   - Go to `http://localhost:3000/architecture-1` -> Click **Sign in**.
   - On the blue central broker page, click **Staff**.
   - On the green internal page, enter `alice` / `password`.
   - Verify on Identity Card: `Decided by: roles claim`, `Reason: roles contains internal_user`, `Roles claim: ["internal_user"]`.
   - Add an internal note on ticket T-1 -> Note appears immediately.
2. **External Path**:
   - Click **Sign out** -> Click **Sign in**.
   - On the blue central broker page, click **Partner**.
   - On the orange external page, enter `xavier` / `password`.
   - Verify on Identity Card: `Decided by: roles claim`, `Reason: roles contains external_resolver`, `Roles claim: ["external_resolver"]`.
   - Attempt to add an internal note -> Fails with HTTP 403.
3. **Fail-Closed Path**:
   - In Keycloak Admin (`http://localhost:8080/admin`), realm `central`, delete identity provider mapper `external-gets-external_resolver`.
   - Sign in as `xavier` -> App displays `Sign-in rejected: User rejected (fail closed): The ID token did not contain an authorized app role`.
