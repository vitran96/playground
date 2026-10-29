# OIDC Multi-Provider Architecture Demo (7 Reference Architectures)

A production-grade local playground demonstrating and comparing **7 different architectural patterns** for integrating Single Sign-On (SSO) with multiple user classifications: **Internal Staff** (intranet) and **External Partners** (third-party resolvers) using OpenID Connect (OIDC), Keycloak, Traefik, and a Node.js/Hono application.

---

## Architecture Comparison Matrix

| # | Architecture Pattern | Ingress / Gateway | Provider Model | Decision Point | User Experience | Best For |
|---|----------------------|-------------------|----------------|----------------|-----------------|----------|
| **1** | **Central Broker** (`/architecture-1`) | Direct (:3000) | 1 Central Broker (`kc-central`) | `roles` claim in ID token | User clicks "Staff" or "Partner" on broker page | Standard enterprise federation with centralized role governance |
| **2** | **Direct Providers** (`/architecture-2`) | Direct (:3000) | 2 Direct IdPs (`kc-internal`, `kc-external`) | Callback route + token `iss` | User clicks dedicated login button in app UI | Independent identity providers without broker infrastructure |
| **3** | **Dual Traefik Ingress** (`/architecture-3`) | 2 Traefiks (:8083, :8084) | 2 Direct IdPs | Network gateway origin | Internal URL auto-redirects to staff IdP; external URL prompts partner login | Strict DMZ vs Intranet physical network isolation |
| **4** | **Single Traefik Domain** (`/architecture-4`) | 1 Traefik (:8085, :8086) | 2 Direct IdPs | Frontend `window.location.port` | Port 8085 triggers staff login; port 8086 renders partner login | Multi-tenant single proxy with separate internal/external DNS hostnames |
| **5** | **Sub-Route Scoping** (`/architecture-5`) | Direct (:3000) | 2 Direct IdPs | URL Sub-path (`/internal` vs root) | Staff portal mounted under `/internal`; partner portal under root `/architecture-5` | Unified portal domain with path-based access control separation |
| **6** | **Gateway Header Evaluation** (`/architecture-6`) | 2 Traefiks (:8083, :8084) | 2 Direct IdPs | Injected `X-Network-Zone` header | Single unified `/auth/a6/login` route; backend routes automatically | Zero-client-logic gateway-driven routing |
| **7** | **Central Broker + IdP Hint** (`/architecture-7`) | Direct / Traefik | 1 Central Broker + `kc_idp_hint` | `kc_idp_hint` + `roles` claim | Direct login at target realm with centralized broker token & role mapping | Eliminates broker selection screen while preserving central token federation |

---

## Directory Structure

```text
.
├── app/
│   ├── public/
│   │   ├── index.html            # Light theme Pico CSS v2 layout
│   │   └── app.js                # Preact SPA (HTM, zero-build, 7 architecture screens)
│   ├── src/
│   │   ├── server.js             # Hono HTTP server, static asset delivery & session middleware
│   │   ├── oidc.js               # OIDC Discovery, PKCE S256 code exchange & dynamic redirectUri
│   │   ├── classify.js           # Server-side user classification logic
│   │   ├── config.js             # OIDC provider configuration (central, internal, external)
│   │   ├── users.js              # In-memory user store
│   │   ├── tickets.js            # Sample ticket service with role-based access control
│   │   ├── arch1.js              # Architecture 1 router
│   │   ├── arch2.js              # Architecture 2 router
│   │   └── arch_extra.js         # Router for Architectures 3, 4, 5, 6, and 7
│   ├── package.json
│   └── Dockerfile
├── keycloak/
│   ├── realms/
│   │   ├── central.json          # Central Broker realm definition (roles, identity provider mappers)
│   │   ├── internal.json         # Corp Intranet realm (alice, bob)
│   │   └── external.json         # Partner Portal realm (xavier, yen)
│   └── themes/                   # Custom light-theme login pages (Central blue, Internal green, External orange)
├── traefik/
│   ├── internal.yaml             # Traefik internal dynamic config (injects X-Network-Zone: internal)
│   ├── external.yaml             # Traefik external dynamic config (injects X-Network-Zone: external)
│   └── single.yaml               # Traefik single dynamic config (ports 8085 and 8086)
├── scripts/
│   └── smoke.mjs                 # Automated non-interactive smoke test suite
├── compose.yaml                  # Multi-container orchestration (Keycloak x3, Traefik x3, App)
├── INSTRUCTIONS.md               # Universal AI assistant & developer instructions
├── CLAUDE.md                     # Claude Code workspace guidelines
├── .github/
│   └── copilot-instructions.md   # GitHub Copilot workspace guidelines
├── architecture-1.instruction.md # Comprehensive guide for Architecture 1
├── architecture-2.instruction.md # Comprehensive guide for Architecture 2
├── architecture-3.instruction.md # Comprehensive guide for Architecture 3
├── architecture-4.instruction.md # Comprehensive guide for Architecture 4
├── architecture-5.instruction.md # Comprehensive guide for Architecture 5
├── architecture-6.instruction.md # Comprehensive guide for Architecture 6
└── architecture-7.instruction.md # Comprehensive guide for Architecture 7
```

---

## Quickstart

### Prerequisites
- Docker & Docker Compose (or Podman / Podman Compose).
- Node.js (v20+) if running smoke tests or inspecting code locally.

### 1. Setup Environment
```bash
cp .env.example .env
```

### 2. Start Services
```bash
docker compose up --build
```
*(Wait until the three Keycloak instances report healthy and Traefik gateways bind.)*

### 3. Open the Demo App
- Direct Application: [http://localhost:3000](http://localhost:3000)

---

## Ports & Gateway Endpoints

| Service | Port | Description |
|---------|------|-------------|
| **Demo App** | `http://localhost:3000` | Main frontend and backend service |
| **Traefik Internal** | `http://localhost:8083` | Reverse proxy injecting `X-Network-Zone: internal` (Arch 3 & 6) |
| **Traefik External** | `http://localhost:8084` | Reverse proxy injecting `X-Network-Zone: external` (Arch 3 & 6) |
| **Traefik Single (Internal)** | `http://localhost:8085` | Dual-port Traefik internal entrypoint (Arch 4) |
| **Traefik Single (External)** | `http://localhost:8086` | Dual-port Traefik external entrypoint (Arch 4) |
| **Central Keycloak** | `http://localhost:8080` | Broker Realm (`/realms/central`) |
| **Internal Keycloak** | `http://localhost:8081` | Staff Realm (`/realms/internal`) |
| **External Keycloak** | `http://localhost:8082` | Partner Realm (`/realms/external`) |

---

## Demo Credentials

All test users share the password: **`password`**

| Realm | Username | Name | Email | System Role | Accessible Scope |
|-------|----------|------|-------|-------------|------------------|
| **internal** | `alice` | Alice Nguyen | `alice@corp.example` | Internal Triage | All tickets (T-1 to T-4), internal notes permitted |
| **internal** | `bob` | Bob Tran | `bob@corp.example` | Internal Resolver | All tickets (T-1 to T-4), internal notes permitted |
| **external** | `xavier` | Xavier Le | `xavier@partner-a.example` | External Resolver (Partner A) | Assigned tickets (T-2) with shared comments only |
| **external** | `yen` | Yen Pham | `yen@partner-b.example` | External Resolver (Partner B) | Assigned tickets (T-3) with shared comments only |

**Keycloak Admin Console**: `admin` / `admin` on ports 8080, 8081, and 8082.

---

## Verification & Acceptance Criteria

### Architecture 1: Central Broker
1. Navigate to `/architecture-1` -> Click **Sign in**.
2. Keycloak Central presents exactly two options: **Staff (intranet sign-in)** or **Partner sign-in**.
3. Select **Staff** -> Login as `alice` / `password`.
4. Returned to `/architecture-1`: Identity card displays `userType: internal`, decided by `roles claim`, `roles` contains `internal_user`, sees all tickets.
5. Logout -> Click **Sign in** -> Select **Partner** -> Login as `xavier` / `password`.
6. Returned: `userType: external`, decided by `roles claim`, `roles` contains `external_resolver`, sees only T-2.

### Architecture 2: Direct Providers
1. Navigate to `/architecture-2`.
2. Notice two distinct buttons: **Sign in as internal** and **Sign in as external**.
3. Click **Sign in as internal** -> Directly reaches green internal realm -> Login as `bob` / `password`.
4. Returned: Decided by `login route + issuer`, login route `/auth/a2/internal/callback`.
5. Click **Sign out** -> Click **Sign in as external** -> Orange external realm -> Login as `yen` / `password`.
6. Returned: Decided by `login route + issuer`, login route `/auth/a2/external/callback`.

### Architecture 3: Dual Traefik Gateways
1. Access via Internal Traefik: [http://localhost:8083/architecture-3](http://localhost:8083/architecture-3).
2. Click **Sign in as internal** -> Authenticates against `kc-internal`.
3. Access via External Traefik: [http://localhost:8084/architecture-3](http://localhost:8084/architecture-3).
4. Click **Sign in as external** -> Authenticates against `kc-external`.

### Architecture 4: Single Traefik with Port/Domain Routing
1. Access internal port: [http://localhost:8085/architecture-4](http://localhost:8085/architecture-4).
2. Frontend detects port `8085` -> Highlights internal login option -> Authenticates via `kc-internal`.
3. Access external port: [http://localhost:8086/architecture-4](http://localhost:8086/architecture-4).
4. Frontend detects port `8086` -> Highlights partner login option -> Authenticates via `kc-external`.

### Architecture 5: Sub-Route Differentiation
1. Access internal portal: [http://localhost:3000/architecture-5/internal](http://localhost:3000/architecture-5/internal).
2. Sign in with `alice` -> Session and tickets mounted under `/internal`.
3. Switch to external portal: [http://localhost:3000/architecture-5](http://localhost:3000/architecture-5).
4. Demonstrates path-level boundary isolation on a single domain.

### Architecture 6: Injected Gateway Header (`X-Network-Zone`)
1. Access via Internal Traefik: [http://localhost:8083/architecture-6](http://localhost:8083/architecture-6).
2. UI displays `X-Network-Zone: internal`. Click unified **Unified Sign In (/auth/a6/login)**.
3. Backend automatically inspects header and redirects to `kc-internal`.
4. Repeat via External Traefik: [http://localhost:8084/architecture-6](http://localhost:8084/architecture-6). Backend routes to `kc-external`.

### Architecture 7: Central Broker with IdP Hint (`kc_idp_hint`)
1. Navigate to `/architecture-7`.
2. Click **Sign in with Internal Hint**.
3. App requests `kc-central` with `kc_idp_hint=internal` -> Keycloak automatically skips the broker picker and lands on the internal staff login page.
4. Token returned to App contains verified `roles` from the central broker.

---

## Automated Smoke Tests

Run the non-interactive automated test suite:
```bash
node scripts/smoke.mjs
```

The smoke test verifies:
- OIDC Discovery endpoints across all 3 Keycloak realms.
- Unauthenticated API 401 guard responses across all architectures.
- PKCE `S256` parameter generation and code challenge verification.
- Architecture 6 header-based redirect evaluation (`X-Network-Zone`).
- Architecture 7 IdP hint injection (`kc_idp_hint`).

---

## Resetting State

To stop all containers and wipe state:
```bash
docker compose down -v
```
