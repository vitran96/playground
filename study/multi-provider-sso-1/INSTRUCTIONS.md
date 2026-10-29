# Project Instructions: Multi-Provider OIDC SSO Architecture Demo

This document outlines the standard architecture, technical constraints, coding standards, and operational guidelines for developers and AI coding agents (Claude, GitHub Copilot, Gemini, Cursor).

---

## 1. Project Overview & Philosophy

This project is a multi-provider Single Sign-On (SSO) architectural study demonstrating **7 distinct patterns** for integrating and separating **Internal Staff** (intranet) and **External Partners** (third-party resolvers) using OpenID Connect (OIDC).

### Core Design Philosophy
- **Zero-Build Frontend**: The frontend is written using Preact with `htm` tagged template literals in pure ES modules. There is no Vite, Webpack, Babel, or bundling step. Static files are served directly by Hono.
- **Lightweight, Native Standards**: Built on native platform capabilities: standard `fetch`, standard URL APIs, standard crypto, and Pico CSS v2.
- **Security-First (Fail Closed)**: Missing claims, unrecognized roles, unverified issuers, or state mismatches must always fail closed (reject authentication, abort session creation).
- **Concise, High-Reusability Code**: Backend CRUD endpoints use factory helpers (`mountCrud`); frontend screens use shared UI templates (`GenericArchScreen`).

---

## 2. Tech Stack & Boundaries

| Component | Technology | Version / Specifics |
|-----------|------------|---------------------|
| **Backend Runtime** | Node.js (ES Modules only: `"type": "module"`) | v20+ |
| **Web Framework** | Hono + `@hono/node-server` | Hono v4 |
| **OIDC / JWT** | `jose` | Remote JWKS verification, RS256, PKCE S256 |
| **Frontend Framework** | Preact via `htm/preact/standalone.module.js` | Zero compile step |
| **CSS System** | Pico CSS v2 (CDN) | Light theme (`data-theme="light"`) |
| **Color Scheme** | Primary: `#393939`, Secondary: `#7E6C39`, Body: `#ffffff` | Header/footer `#393939`, buttons `#7E6C39` |
| **Identity Broker & IdPs** | Keycloak | 26.4 (Central: 8080, Internal: 8081, External: 8082) |
| **Reverse Proxies** | Traefik | v3.1 (Internal: 8083, External: 8084, Dual: 8085/8086) |

---

## 3. Directory Layout & Key Locations

```text
app/
  public/
    index.html        # App skeleton, Pico CSS v2 stylesheet, color tokens
    app.js            # Frontend single-page app (Preact + htm), 7 arch views
  src/
    server.js         # Entrypoint, route mounting, session middleware
    oidc.js           # OIDC client: discovery, PKCE S256, code exchange, logout
    classify.js       # Server-side classification logic (roles vs route+iss)
    config.js         # Provider configurations and credentials
    users.js          # In-memory identity database
    tickets.js        # RBAC ticket data store (internal vs external views)
    arch1.js          # Architecture 1 routes
    arch2.js          # Architecture 2 routes
    arch_extra.js     # Architectures 3, 4, 5, 6, 7 routes & mountCrud helper
compose.yaml          # Podman / Docker compose file defining all services
traefik/              # Traefik configuration files (internal, external, single)
keycloak/realms/      # Realm export JSON files (central, internal, external)
scripts/smoke.mjs     # Automated non-interactive test suite
```

---

## 4. Coding & Architecture Standards

### 4.1 OIDC Security Invariants
- **Always use PKCE S256**: The client must generate high-entropy random `verifier` and `challenge = sha256(verifier).digest('base64url')`.
- **Always validate OAuth state & nonce**: Every login initiates with a random `state` and `nonce` stored in the session. Mismatches must immediately return HTTP 400.
- **Token Signature Verification**: Verify ID tokens using `jose.jwtVerify()` against the provider's remote JWKS endpoint.
- **Fail Closed**: In Architecture 1 & 7 (role-based classification), if `roles` does not contain exactly one authorized app role (`internal_user` or `external_resolver`), reject login with `?error=no_role`.

### 4.2 Dynamic Ingress Host Resolution
When running behind Traefik proxies (`:8083`, `:8084`, `:8085`, `:8086`), the application must compute `redirectUri` dynamically from incoming headers:
```javascript
export function getAppBase(c) {
  const host = c.req.header('x-forwarded-host') || c.req.header('host') || 'localhost:3000';
  const proto = c.req.header('x-forwarded-proto') || 'http';
  return `${proto}://${host}`;
}
```
Store the initiated `redirectUri` in `session.pending[providerKey]` so that token exchange passes the identical URI that was registered during authorization.

### 4.3 Styling & UI Rules
- Keep `<html lang="en" data-theme="light">` active.
- Body background must remain white (`#ffffff`).
- Header, sidebar, and footer elements use primary dark charcoal (`#393939`) with light text.
- Action buttons, interactive highlights, and secondary elements use secondary olive gold (`#7E6C39`).
- Maintain the internal (green `#2e7d32`) and external (orange `#d84315`) badges/buttons where provider identity distinction is emphasized.

---

## 5. Architecture Reference Files

For in-depth context, flowcharts, security threat models, and code walkthroughs of each architecture, consult the corresponding instruction file:
- `architecture-1.instruction.md`: Central Identity Broker Pattern
- `architecture-2.instruction.md`: Direct Multiple Identity Providers Pattern
- `architecture-3.instruction.md`: Dual Ingress Reverse Proxies (Network Isolation)
- `architecture-4.instruction.md`: Single Reverse Proxy with Client Domain/Port Routing
- `architecture-5.instruction.md`: Sub-Route Path Scoping Pattern
- `architecture-6.instruction.md`: Gateway-Injected Header Evaluation Pattern
- `architecture-7.instruction.md`: Central Broker with Automated IdP Hint Pattern

---

## 6. Development & Testing Commands

### Run App Outside Container
```bash
cd app
npm install
npm start
```

### Run Smoke Test Suite
```bash
node scripts/smoke.mjs
```

### Full Container Lifecycle
```bash
# Start all 7 services
docker compose up --build

# Wipe volumes and containers
docker compose down -v
```
