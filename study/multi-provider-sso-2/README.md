# Keycloak Login Architectures – Evaluation Reference Demo

A runnable demo comparing four enterprise login patterns using two Keycloaks, Apache reverse-proxy gateways (`mod_auth_openidc`), a decoupled Hono backend, and a static Preact frontend.

> [!WARNING]
> **Development Demo Only:** This repository contains hardcoded test credentials and development secrets in `.env.example`. It is designed as a comparative reference sample for architecture evaluation and is **not production-hardened**.

---

## 1. Quick Start

### Prerequisites
- Docker & Docker Compose (or Podman & Podman Compose)
- Linux / macOS / Windows with WSL2

### DNS Note for Local Hostnames
Modern browsers (Chrome, Edge, Firefox) resolve `*.localhost` to `127.0.0.1` automatically. If your local environment does not resolve subdomain entries, add the following line to `/etc/hosts` (or `C:\Windows\System32\drivers\etc\hosts`):

```text
127.0.0.1 int.localhost ext.localhost
```

### Starting the Demo
1. Create environment file:
   ```bash
   cp .env.example .env
   ```
2. Start all services:
   ```bash
   docker compose up --build
   ```
   *(Wait until `kc2-init` finishes with exit code 0 and all gateways report healthy).*

3. Open the **Evaluation Portal**:
   👉 **`http://localhost:8080`**

---

## 2. Architecture Matrix & Service Ports

| # | Architecture | Browser Entry Point | Gateway ID | IdP Used | Login Behavior |
|---|---|---|---|---|---|
| **Portal** | Evaluation Portal | `http://localhost:8080` | – | – | Landing page, architecture matrix, & credentials |
| **1** | Single Gateway: Email-First | `http://localhost:8101/architecture-1/` | `gw-1` | keycloak-2 -> keycloak-1 | User enters email; `@internal.email.com` routed to keycloak-1 |
| **2** | Single Gateway: IdP Choice | `http://localhost:8201/architecture-2/` | `gw-2` | keycloak-2 / keycloak-1 | Two buttons in frontend; gateway injects `kc_idp_hint` |
| **3 (Int)** | Two Domains: Internal | `http://int.localhost:8301/architecture-3/` | `gw-3-int` | keycloak-1 | Direct to company IdP (zero-click Kerberos/SPNEGO) |
| **3 (Ext)** | Two Domains: External | `http://ext.localhost:8302/architecture-3/` | `gw-3-ext` | keycloak-2 | Direct to partner IdP (email/password) |
| **4 (Int)** | App Auth: Internal | `http://int.localhost:8401/architecture-4/` | `gw-4-int` | keycloak-1 | Direct to company IdP (zero-click Kerberos/SPNEGO) |
| **4 (Ext)** | App Auth: External | `http://ext.localhost:8402/architecture-4/` | `gw-4-ext` | None (App-owned) | App-owned login/signup/reset; Keycloak bypassed |

### Infrastructure Admin Consoles & Mail
- **keycloak-1 Admin Console:** `http://localhost:8081/admin` (`admin` / `admin`)
- **keycloak-2 Admin Console:** `http://localhost:8082/admin` (`admin` / `admin`)
- **Mailpit Web UI (Keycloak SMTP):** `http://localhost:8025`

---

## 3. Test Users & Credentials

| Identity Store | Username / Email | Password | Assigned Roles | Used In |
|---|---|---|---|---|
| `keycloak-1` (`internal`) | `alice` (`alice@internal.email.com`) | `alice` | `employee`, `app-admin` | Arch 1–4 (Internal) |
| `keycloak-1` (`internal`) | `bob` (`bob@internal.email.com`) | `bob` | `employee` | Arch 1–4 (Internal) |
| `keycloak-2` (`external`) | `carol@partner.example.com` | `carol` | `partner` | Arch 1–3 (External) |
| `keycloak-2` (`external`) | `dave@customer.example.com` | `dave` | `partner` | Arch 1–3 (External) |
| Backend Store (`users.ts`) | `erin@partner.example.com` | `erin` | `partner` | Arch 4 (External only) |

---

## 4. Acceptance & Security Verification

Run the automated acceptance suite:

```bash
bash scripts/check.sh
```

### Manual Security Checks

1. **Gateway Boundary Isolation:**
   Call Architecture 1 from Gateway 2:
   ```bash
   curl -i http://localhost:8201/api/architecture-1/me
   # Expected: HTTP 404 (Apache proxy restriction)
   ```

2. **Cross-Zone Token Replay:**
   Log into `ext.localhost:8302/architecture-3/` as `carol`. Copy the session cookie `architecture-3-external-session`.
   Attempt to access the internal domain:
   ```bash
   curl -i -H "Cookie: architecture-3-internal-session=<TOKEN>" http://int.localhost:8301/api/architecture-3/me
   # Expected: HTTP 401 (Audience mismatch: 'architecture-3:external' != 'architecture-3:internal')
   ```

3. **Architecture 4 Local Auth Isolation:**
   Attempt to invoke local auth from the internal gateway:
   ```bash
   curl -i -X POST http://int.localhost:8401/api/architecture-4/local/login
   # Expected: HTTP 403 (Gateway validation restricts local auth to gw-4-ext)
   ```

---

## 5. Clean Up

To stop and reset all containers and volumes:

```bash
docker compose down -v
```
