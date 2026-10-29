# Architecture 6: Gateway Header Injected Evaluation Pattern

## 1. Overview & Concept
Architecture 6 shifts provider routing decisions from the frontend UI to the **backend server**, guided by trusted headers injected by the network ingress gateway:

1. **Ingress Injects Header**:
   - `traefik-internal` (:8083) injects `X-Network-Zone: internal`.
   - `traefik-external` (:8084) injects `X-Network-Zone: external`.
2. **Unified Client Endpoint**:
   The frontend application and client code use a single, unified sign-in URL: `GET /auth/a6/login`.
3. **Backend Evaluates & Redirects**:
   The backend inspects `c.req.header('x-network-zone')`:
   - If `internal`: automatically initiates OIDC code flow with `kc-internal`.
   - If `external`: automatically initiates OIDC code flow with `kc-external`.

```
[ Ingress Gateway ] ──(Injects X-Network-Zone: internal/external)──> [ GET /auth/a6/login ]
                                                                             │
                                                                   [ Backend inspects header ]
                                                                             │
                                   ┌─────────────────────────────────────────┴─────────────────────────────────────────┐
                                   ▼                                                                                     ▼
                      (Header is 'internal')                                                                (Header is 'external')
                                   │                                                                                     │
                                   ▼                                                                                     ▼
                        [ 302 to kc-internal:8081 ]                                                           [ 302 to kc-external:8082 ]
```

---

## 2. Real-World Context & Trade-Offs

### When to Choose This Pattern
- Enterprise architectures with centralized API Gateways (Traefik, Kong, Envoy, AWS API Gateway, NGINX).
- Zero Trust architectures where network boundary metadata (VPC peering, client IP location, VPN status) is validated and normalized by ingress proxies into trusted internal headers.
- Applications seeking a frictionless user experience with zero provider-selection UI on the frontend.

### Advantages
- **Single Client Endpoint**: Client code and bookmarks only need to know `/auth/a6/login`.
- **Tamper-Resistant**: Ingress proxies strip untrusted client-supplied `X-Network-Zone` headers at the network edge and inject verified zone context.
- **Centralized Security Policy**: If network routing rules change, only gateway configuration or backend router logic changes, without altering frontend builds.

### Disadvantages & Risks
- **Header Spoofing Risk**: If the application backend is directly accessible bypassing the ingress proxy, attackers could spoof `X-Network-Zone: internal`. The backend must only trust this header if incoming traffic is restricted to the proxy network.
- **Direct Access Fallback**: If accessed directly on port 3000 without the proxy header, the backend must define a safe fallback strategy.

---

## 3. Topology & Configuration

| Ingress Proxy | Gateway Port | Injected Header | Backend Target |
|---------------|--------------|-----------------|----------------|
| **Traefik Internal** | `http://localhost:8083` | `X-Network-Zone: internal` | `http://app:3000` |
| **Traefik External** | `http://localhost:8084` | `X-Network-Zone: external` | `http://app:3000` |

### Traefik Middleware (`traefik/internal.yaml`)
```yaml
http:
  middlewares:
    internal-header:
      headers:
        customRequestHeaders:
          X-Network-Zone: "internal"
```

---

## 4. End-to-End Authentication Flow

1. **Client Action**: User visits `/architecture-6` and clicks **Unified Sign In (/auth/a6/login)**.
2. **Backend Header Inspection**:
   In `app/src/arch_extra.js`:
   ```javascript
   archExtraRouter.get('/auth/a6/login', (c) => {
     const zone = c.req.header('x-network-zone') || c.req.query('zone') || 'internal';
     if (zone === 'external') {
       return startLogin(c, config.providers['a2-external'], { callbackPath: '/auth/a6/callback' });
     }
     return startLogin(c, config.providers['a2-internal'], { callbackPath: '/auth/a6/callback' });
   });
   ```
3. **IdP Redirection**:
   - If accessed via `:8083`, browser lands directly on Corp Intranet realm (`kc-internal`).
   - If accessed via `:8084`, browser lands directly on Partner Portal realm (`kc-external`).
4. **Callback & Enforcement**:
   - Return URL is `/auth/a6/callback`.
   - Backend exchanges code with matching provider and establishes session `a6`.
   - Reason recorded: `backend evaluated X-Network-Zone: internal (or external)`.

---

## 5. Security & Verification Script

1. **Verify Internal Header Evaluation**:
   - Navigate to `http://localhost:8083/architecture-6`.
   - Observe real-time header detection: `Detected Header: X-Network-Zone: internal`.
   - Click **Unified Sign In (/auth/a6/login)**.
   - Browser lands directly on green internal Keycloak login page (zero provider selection prompt).
   - Login as `alice` / `password`.
   - Identity Card displays: `Decided by: backend header evaluation`, `Reason: backend evaluated X-Network-Zone: internal`.
2. **Verify External Header Evaluation**:
   - Navigate to `http://localhost:8084/architecture-6`.
   - Observe header detection: `Detected Header: X-Network-Zone: external`.
   - Click **Unified Sign In (/auth/a6/login)**.
   - Browser lands directly on orange external Keycloak login page.
   - Login as `xavier` / `password`.
   - Identity Card displays: `Decided by: backend header evaluation`, `Reason: backend evaluated X-Network-Zone: external`.
