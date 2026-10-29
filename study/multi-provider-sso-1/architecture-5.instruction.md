# Architecture 5: Sub-Route Differentiation Pattern

## 1. Overview & Concept
Architecture 5 eliminates custom proxy configurations, extra gateway ports, and multi-domain setups by using **URL path hierarchy** as the tenant and identity boundary:

- **Internal Portal (`/architecture-5/internal`)**: All staff user interactions, internal login flows (`/auth/a5/internal/login`), internal sessions (`a5_internal`), and internal APIs (`/api/a5/internal/*`) are mounted under the `/internal` sub-path.
- **External Portal (`/architecture-5`)**: The parent root route serves external partner users, external login flows (`/auth/a5/external/login`), external sessions (`a5_external`), and partner APIs (`/api/a5/*`).

```
                                  [ Single Host (localhost:3000) ]
                                                 │
                   ┌─────────────────────────────┴─────────────────────────────┐
                   ▼                                                           ▼
       [ Route: /architecture-5 ]                                 [ Route: /architecture-5/internal ]
       - Partner Login (kc-external)                              - Staff Login (kc-internal)
       - Scope: External Resolver                                 - Scope: Internal Staff
       - API: /api/a5/*                                           - API: /api/a5/internal/*
       - Session key: a5_external                                 - Session key: a5_internal
```

---

## 2. Real-World Context & Trade-Offs

### When to Choose This Pattern
- Standard web monoliths or microfrontends hosted under a single root domain (e.g., `https://portal.company.com` for clients and `https://portal.company.com/internal` for employees).
- Cookie path-scoping architectures where session cookies must be partitioned by path (`Path=/internal`).
- Organizations with strict single-domain TLS wildcard certificate limitations.

### Advantages
- **Zero Ingress / Gateway Dependency**: Works seamlessly on standard port 3000 with zero reverse proxies or load balancer routing rules.
- **Standard Framework Routing**: Path routing is native to every web framework (Hono, Express, Next.js, Django, Spring).
- **Independent Session Scopes**: A developer can be authenticated simultaneously as internal in `/internal` and external in the root portal without cookie collisions.

### Disadvantages & Risks
- **Path Traversal / Route Leakage**: Strict route guards must ensure that non-internal users cannot access `/internal/*` routes or APIs.
- **SEO & Discovery**: Internal paths may be accidentally indexed or exposed in public sitemaps if not guarded by robot policies and authentication middlewares.

---

## 3. Topology & Configuration

| Route / Sub-Path | Authentication Endpoint | API Endpoints | Session Key | Target IdP |
|------------------|-------------------------|---------------|-------------|------------|
| `/architecture-5` | `/auth/a5/external/login` | `/api/a5/me`, `/api/a5/tickets` | `a5_external` | `kc-external` (8082) |
| `/architecture-5/internal` | `/auth/a5/internal/login` | `/api/a5/internal/me`, `/api/a5/internal/tickets` | `a5_internal` | `kc-internal` (8081) |

---

## 4. End-to-End Authentication Flow

1. **Internal Staff Flow**:
   - Staff member visits `http://localhost:3000/architecture-5/internal`.
   - UI renders internal portal view with **Sign in to Internal Portal** button.
   - User clicks button -> requests `GET /auth/a5/internal/login`.
   - App redirects to `http://localhost:8081/realms/internal`.
   - On successful login, Keycloak redirects back to `/auth/a5/internal/callback`.
   - Backend sets `session.a5_internal` with `userType: 'internal'`.
   - User is redirected back to `/architecture-5/internal`.
   - API calls target `/api/a5/internal/tickets`.
2. **External Partner Flow**:
   - Partner visits root `http://localhost:3000/architecture-5`.
   - UI renders external portal view with **Sign in to External Portal** button.
   - User clicks button -> requests `GET /auth/a5/external/login`.
   - App redirects to `http://localhost:8082/realms/external`.
   - On successful login, Keycloak redirects back to `/auth/a5/external/callback`.
   - Backend sets `session.a5_external` with `userType: 'external'`.
   - User is redirected back to `/architecture-5`.
   - API calls target `/api/a5/tickets`.

---

## 5. Security & Verification Script

1. **Verify Internal Sub-Route**:
   - Open `http://localhost:3000/architecture-5/internal`.
   - Verify header says `Internal Portal (/internal)`.
   - Click **Sign in to Internal Portal** -> Login as `alice` / `password`.
   - Identity Card displays `userType: internal`, `Decided by: sub-route /internal`.
   - Adding internal note on ticket succeeds.
2. **Verify Root External Route**:
   - Open `http://localhost:3000/architecture-5`.
   - Notice the external portal is unauthenticated (clean session isolation from the internal portal).
   - Click **Sign in to External Portal** -> Login as `xavier` / `password`.
   - Identity Card displays `userType: external`, `Decided by: parent root route`.
   - Adding internal note fails with HTTP 403 Forbidden.
3. **Verify Simultaneous Coexistence**:
   - Open two tabs: Tab 1 at `/architecture-5/internal` (logged in as `alice`) and Tab 2 at `/architecture-5` (logged in as `xavier`).
   - Refresh both tabs: both sessions remain active without overwriting each other.
