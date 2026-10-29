# Architecture 4: Single Reverse Proxy with Frontend Domain/Port Decision

## 1. Overview & Concept
Architecture 4 consolidates the reverse proxy infrastructure into a **single Traefik instance** (`traefik-single`), but configures multiple entrypoints (ports in local development, or domain names in production, e.g. `staff.corp.example` vs `partners.corp.example`).

The **Frontend client** inspects the current origin (`window.location.port` or `window.location.hostname`) to determine the user experience:
- If accessed via the internal domain/port (`:8085`): Frontend detects internal context and triggers automatic redirection to the internal Keycloak realm.
- If accessed via the external domain/port (`:8086`): Frontend presents the external partner login form with email & password fields.

```
[ User on port :8085 (Internal) ] ──┐
                                     ├──> [ Single Traefik:v3.1 ] ──> [ App:3000 ]
[ User on port :8086 (External) ] ──┘                                    │
                                                                         ▼
                                                     [ Frontend reads window.location.port ]
                                                                         │
                                       ┌─────────────────────────────────┴─────────────────────────────────┐
                                       ▼                                                                   ▼
                         (Port 8085 -> kc-internal)                                          (Port 8086 -> kc-external)
```

---

## 2. Real-World Context & Trade-Offs

### When to Choose This Pattern
- SaaS or cloud environments utilizing a single application load balancer (AWS ALB, Google Cloud HTTPS Load Balancer, Cloudflare) with multiple DNS CNAMEs pointing to the same service.
- Single Page Applications (SPAs) where client-side customization per tenant/domain is already standard practice.
- Scenarios where infrastructure teams want minimal gateway complexity (1 proxy container) while maintaining domain-driven UX separation.

### Advantages
- **Single Proxy Footprint**: Only one Traefik instance to deploy, manage, and scale.
- **Dynamic Frontend Branding**: The SPA can dynamically alter branding, logos, help text, and color schemes based on the host domain.
- **Clean DNS Mapping**: Production domains like `staff.mycompany.com` and `partners.mycompany.com` route cleanly to their expected IdP without custom backend middleware.

### Disadvantages & Risks
- **Client-Side Dependency**: The initial routing decision relies on client-side JavaScript execution.
- **Security Boundary**: Domain detection in the browser is a UX optimization, **not an authorization boundary**. The backend must still cryptographically verify token issuers and roles.

---

## 3. Topology & Configuration

| Component | Port / Entrypoint | Domain Role |
|-----------|-------------------|-------------|
| **Traefik Single** | `http://localhost:8085` | Entrypoint `internal` (Staff portal domain) |
| **Traefik Single** | `http://localhost:8086` | Entrypoint `external` (Partner portal domain) |
| **App Service** | `http://app:3000` | Unified frontend & backend service |
| **Internal Keycloak** | `http://localhost:8081` | Corp Intranet realm |
| **External Keycloak** | `http://localhost:8082` | Partner realm |

### Traefik Configuration (`traefik/single.yaml`)
```yaml
http:
  routers:
    internal-router:
      entryPoints: ["internal"]
      rule: "PathPrefix(`/`)"
      service: "app-service"
    external-router:
      entryPoints: ["external"]
      rule: "PathPrefix(`/`)"
      service: "app-service"
  services:
    app-service:
      loadBalancer:
        servers:
          - url: "http://app:3000"
```

---

## 4. End-to-End Authentication Flow

1. **Client Domain Detection**:
   In `app/public/app.js`:
   ```javascript
   const port = window.location.port;
   const isInternal = port === '8085';
   const isExternal = port === '8086';
   ```
2. **Behavioral Branching**:
   - On port `8085`: The UI displays `Internal port matched! Directing to internal Keycloak.` with a direct action button to `/auth/a4/internal/login`.
   - On port `8086`: The UI displays `External port matched! Presenting partner login.` with a partner login action to `/auth/a4/external/login`.
3. **Backend Authorization & Code Exchange**:
   - Handled via `archExtraRouter` (`/auth/a4/internal/*` and `/auth/a4/external/*`).
   - Dynamic `redirectUri` resolves to `http://localhost:8085/...` or `http://localhost:8086/...`.
   - Backend establishes session `a4` recording `decidedBy: frontend port/domain check`.

---

## 5. Security & Verification Script

1. **Test Port 8085 (Internal)**:
   - Navigate to `http://localhost:8085/architecture-4`.
   - Observe message: `Frontend detected port: 8085 -> Internal port matched!`.
   - Click **Sign in as internal** -> Login as `bob` / `password`.
   - Identity Card displays `userType: internal`, `Decided by: frontend port/domain check (port 8085)`.
2. **Test Port 8086 (External)**:
   - Navigate to `http://localhost:8086/architecture-4`.
   - Observe message: `Frontend detected port: 8086 -> External port matched!`.
   - Click **Sign in as external** -> Login as `yen` / `password`.
   - Identity Card displays `userType: external`, `Decided by: frontend port/domain check (port 8086)`.
