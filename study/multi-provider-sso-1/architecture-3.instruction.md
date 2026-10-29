# Architecture 3: Dual Reverse Proxies (Network Isolation Pattern)

## 1. Overview & Concept
Architecture 3 introduces **infrastructure-level network perimeter segregation**. Instead of exposing a single entrypoint or having the frontend negotiate authentication paths, two completely separate reverse proxy instances front the application:

1. **Internal Traefik Proxy (`traefik-internal` on port 8083)**: Exclusively serves internal intranet traffic. Automatically routes staff to Corp Intranet (`kc-internal`).
2. **External Traefik Proxy (`traefik-external` on port 8084)**: Exclusively serves external partner traffic. Directs partners to external email + password credentials (`kc-external`).

Both reverse proxies forward traffic to the **same underlying application backend** (`http://app:3000`).

```
[ Intranet Client ] ────> [ Traefik Internal:8083 ] ──(Auto-route)──> [ kc-internal:8081 ]
                                     │
                                     ├──> [ App:3000 ]
                                     │
[ External Partner ] ───> [ Traefik External:8084 ] ──(Partner Login)─> [ kc-external:8082 ]
```

---

## 2. Real-World Context & Trade-Offs

### When to Choose This Pattern
- Strict regulatory or security environments (banking, healthcare, defense, government) requiring hard DMZ vs. Intranet physical or virtual network separation.
- Organizations where internal staff connect over corporate VPN / private VPC, while external partners connect over public internet.
- High-security portals where internal admin capabilities must not be routable from the external perimeter under any circumstances.

### Advantages
- **Perimeter Defense**: Network firewalls can block external IP addresses from even connecting to the internal gateway port (:8083).
- **Zero Client Guesswork**: Users cannot select the wrong provider because their physical network path defines their identity provider.
- **Unified Application Core**: The engineering team maintains one single codebase and container deployment; the network topology handles tenant segregation.

### Disadvantages & Risks
- **Infrastructure Overhead**: Requires provisioning, monitoring, and maintaining two separate reverse proxy deployments and ingress controllers.
- **Port / Host Configuration**: Application must dynamically resolve `Host` and `X-Forwarded-*` headers to generate valid OAuth `redirect_uri` targets for both gateways.

---

## 3. Topology & Configuration

| Component | Port | Network Boundary | Function |
|-----------|------|------------------|----------|
| **Traefik Internal** | `http://localhost:8083` | Intranet / Corporate VPN | Reverse proxy injecting `X-Network-Zone: internal` |
| **Traefik External** | `http://localhost:8084` | Public DMZ / Partner Network | Reverse proxy injecting `X-Network-Zone: external` |
| **App Service** | `http://app:3000` | Internal Docker Network | Shared application instance |
| **Internal Keycloak** | `http://localhost:8081` | Intranet IdP | Realm: `internal` |
| **External Keycloak** | `http://localhost:8082` | Partner IdP | Realm: `external` |

### Traefik Configuration Files
- `traefik/internal.yaml`: Proxies all routes to `http://app:3000` with custom header `X-Network-Zone: internal`.
- `traefik/external.yaml`: Proxies all routes to `http://app:3000` with custom header `X-Network-Zone: external`.

---

## 4. End-to-End Authentication Flow

1. **Intranet Path**:
   - User navigates to `http://localhost:8083/architecture-3`.
   - Traefik Internal routes request to `app:3000`.
   - UI reflects connection via Traefik Internal (`:8083`).
   - Clicking **Sign in as internal** triggers `GET /auth/a3/internal/login`.
   - `startLogin()` computes dynamic redirect URI: `http://localhost:8083/auth/a3/internal/callback`.
   - Keycloak authenticates user at `kc-internal` and redirects to `http://localhost:8083/auth/a3/internal/callback`.
   - Session `a3` is created as `internal`.
2. **External Partner Path**:
   - Partner accesses `http://localhost:8084/architecture-3`.
   - Traefik External routes request to `app:3000`.
   - UI reflects connection via Traefik External (`:8084`).
   - Clicking **Sign in as external** triggers `GET /auth/a3/external/login`.
   - Dynamic redirect URI is `http://localhost:8084/auth/a3/external/callback`.
   - Keycloak authenticates user at `kc-external` and redirects to `http://localhost:8084/auth/a3/external/callback`.
   - Session `a3` is created as `external`.

---

## 5. Security & Ingress Invariants

- **Dynamic Base URL Computation**:
  ```javascript
  export function getAppBase(c) {
    const host = c.req.header('x-forwarded-host') || c.req.header('host') || 'localhost:3000';
    const proto = c.req.header('x-forwarded-proto') || 'http';
    return `${proto}://${host}`;
  }
  ```
- **Allowed Keycloak Redirect URIs**: Both `http://localhost:8083/*` and `http://localhost:8084/*` are pre-registered in `keycloak/realms/internal.json` and `external.json`.

---

## 6. Verification Script

1. **Verify Internal Ingress**:
   - Open `http://localhost:8083/architecture-3`.
   - Notice banner: `Current origin: http://localhost:8083 (Traefik Internal Gateway)`.
   - Click **Sign in as internal** -> Sign in as `alice` / `password`.
   - Identity Card displays `Decided by: gateway: internal traefik`.
2. **Verify External Ingress**:
   - Open `http://localhost:8084/architecture-3`.
   - Notice banner: `Current origin: http://localhost:8084 (Traefik External Gateway)`.
   - Click **Sign in as external** -> Sign in as `xavier` / `password`.
   - Identity Card displays `Decided by: gateway: external traefik`.
