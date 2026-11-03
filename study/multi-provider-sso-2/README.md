# Keycloak Login Architectures – Evaluation Reference Demo

A runnable demo comparing four enterprise login patterns using four Keycloaks (keycloak-1 internal; keycloak-2/3/4 external brokers dedicated to Arch 3/1/2), Apache reverse-proxy gateways (`mod_auth_openidc`), a decoupled Hono backend, and a static Preact frontend. A fifth pattern (internal Keycloak exposed to the internet) is documented in the portal for evaluation only.

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
   *(Wait until `kc2-init`, `kc3-init` and `kc4-init` finish with exit code 0 and all gateways report healthy).*

3. Open the **Evaluation Portal**:
   👉 **`http://localhost:8180`**

---

## 2. Architecture Matrix & Service Ports

| # | Architecture | Browser Entry Point | Gateway ID | IdP Used | Login Behavior |
|---|---|---|---|---|---|
| **Portal** | Evaluation Portal | `http://localhost:8180` | – | – | Landing page, architecture matrix, & credentials |
| **1** | Single Gateway: Email-First | `http://localhost:8201/architecture-1/` | `gw-1` | keycloak-3 -> keycloak-1 | User enters email; `@internal.email.com` routed to keycloak-1 |
| **2** | Single Gateway: IdP Choice | `http://localhost:8301/architecture-2/` | `gw-2` | keycloak-4 / keycloak-1 | Two buttons in frontend; gateway injects `kc_idp_hint` |
| **3 (Int)** | Two Domains: Internal | `http://int.localhost:8401/architecture-3/` | `gw-3-int` | keycloak-1 | Direct to company IdP (zero-click Kerberos/SPNEGO) |
| **3 (Ext)** | Two Domains: External | `http://ext.localhost:8402/architecture-3/` | `gw-3-ext` | keycloak-2 | Direct to partner IdP (email/password) |
| **4 (Int)** | App Auth: Internal | `http://int.localhost:8501/architecture-4/` | `gw-4-int` | keycloak-1 | Direct to company IdP (zero-click Kerberos/SPNEGO) |
| **4 (Ext)** | App Auth: External | `http://ext.localhost:8502/architecture-4/` | `gw-4-ext` | None (App-owned) | App-owned login/signup/reset; Keycloak bypassed |
| **5** | Internet-Exposed Internal Keycloak | – (portal "Show more" only) | – | keycloak-1 (public) | Evaluation only; documents SPNEGO / password-form exposure risks |
| **6 (Int)** | App Registration: Internal | `http://int.localhost:8601/architecture-6/` | `gw-6-int` | keycloak-1 | Same as Arch 3 internal |
| **6 (Ext)** | App Registration: External | `http://ext.localhost:8602/architecture-6/` | `gw-6-ext` | keycloak-2 | App login page; Sign In -> keycloak-2, Register in app (temporary password emailed) |
| **7 (Int)** | App Registration + Reset: Internal | `http://int.localhost:8701/architecture-7/` | `gw-7-int` | keycloak-1 | Same as Arch 3 internal |
| **7 (Ext)** | App Registration + Reset: External | `http://ext.localhost:8702/architecture-7/` | `gw-7-ext` | keycloak-2 | Arch 6 + app-owned 6-digit-code password reset (sets password in keycloak-2) |

### Infrastructure Admin Consoles & Mail
- **keycloak-1 Admin Console:** `http://localhost:8181/admin` (`admin` / `admin`)
- **keycloak-2 Admin Console:** `http://localhost:8182/admin` (`admin` / `admin`)
- **keycloak-3 Admin Console (Arch 1, realm `arch1`):** `http://localhost:8183/admin` (`admin` / `admin`)
- **keycloak-4 Admin Console (Arch 2, realm `arch2`):** `http://localhost:8184/admin` (`admin` / `admin`)
- **Mailpit Web UI (Keycloak SMTP):** `http://localhost:8125`

---

## 3. Test Users & Credentials

| Identity Store | Username / Email | Password | Assigned Roles | Used In |
|---|---|---|---|---|
| `keycloak-1` (`internal`) | `alice` (`alice@internal.email.com`) | `alice` | `employee`, `app-admin` | Arch 1–4 (Internal) |
| `keycloak-1` (`internal`) | `bob` (`bob@internal.email.com`) | `bob` | `employee` | Arch 1–4 (Internal) |
| `keycloak-2/3/4` (`external` / `arch1` / `arch2`) | `carol@partner.example.com` | `carol` | `partner` | Arch 1–3 (External) |
| `keycloak-2/3/4` (`external` / `arch1` / `arch2`) | `dave@customer.example.com` | `dave` | `partner` | Arch 1–3 (External) |
| Backend Store (`users.ts`) | `erin@partner.example.com` | `erin` | `partner` | Arch 4 (External only) |

### Registration & Password Reset (external users only)

| Arch | Self-registration | Forgot password | Admin registration (`alice`, role `app-admin`) |
|---|---|---|---|
| 1 / 2 / 3 | Keycloak "Register" link (keycloak-3 / -4 / -2) | Keycloak emails a 6-digit code (Mailpit) | App "Admin Dashboard" creates the user in that arch's external Keycloak |
| 4 | App "Sign Up" tab on `ext.localhost:8502` | App emails a 6-digit code (Mailpit) | App "Admin Dashboard" creates the user in the app store |

- Admin-created users get password `password` and role `partner`; self-registered users get role `partner`.
- `@internal.email.com` addresses are rejected everywhere; internal users come from the company directory.
- Keycloak side is a small SPI in `keycloak/spi` (compiled in `keycloak/Dockerfile`), bound by `keycloak/kc2/init/setup-flows.sh`.
- Existing `.env`? Add the `KC2/3/4_ADMIN_SECRET` lines from `.env.example`.

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
   curl -i http://localhost:8301/api/architecture-1/me
   # Expected: HTTP 404 (Apache proxy restriction)
   ```

2. **Cross-Zone Token Replay:**
   Log into `ext.localhost:8402/architecture-3/` as `carol`. Copy the session cookie `architecture-3-external-session`.
   Attempt to access the internal domain:
   ```bash
   curl -i -H "Cookie: architecture-3-internal-session=<TOKEN>" http://int.localhost:8401/api/architecture-3/me
   # Expected: HTTP 401 (Audience mismatch: 'architecture-3:external' != 'architecture-3:internal')
   ```

3. **Architecture 4 Local Auth Isolation:**
   Attempt to invoke local auth from the internal gateway:
   ```bash
   curl -i -X POST http://int.localhost:8501/api/architecture-4/local/login
   # Expected: HTTP 403 (Gateway validation restricts local auth to gw-4-ext)
   ```

---

## 5. Clean Up

To stop and reset all containers and volumes:

```bash
docker compose down -v
```
