# OIDC Multi-Provider Architecture Demo (1 vs 2 Providers)

A local demo comparing two OIDC patterns for handling internal (staff) and external (partner) users in a single application:
- **Architecture 1 (`/architecture-1`)**: 1 OIDC provider (central broker). User type is decided by the `roles` claim in the ID token.
- **Architecture 2 (`/architecture-2`)**: 2 OIDC providers (direct). User type is decided by which callback route completed the flow, confirmed by the token's `iss`.

## Prerequisites

- Docker and Docker Compose (or Podman / Podman Compose).
- Node.js (v20+) if running smoke tests or local tools outside container.

## How to run

1. Copy the environment configuration:
   ```bash
   cp .env.example .env
   ```

2. Start the services:
   ```bash
   docker compose up --build
   ```
   *(Wait until Keycloak instances and the app report ready.)*

3. Open the demo app:
   [http://localhost:3000](http://localhost:3000)

## Demo Users

All users have the password `password`.

| Realm | Username | Name | Email | Role |
| --- | --- | --- | --- | --- |
| internal | `alice` | Alice Nguyen | alice@corp.example | Internal triage |
| internal | `bob` | Bob Tran | bob@corp.example | Internal resolver |
| external | `xavier` | Xavier Le | xavier@partner-a.example | External resolver (Partner A) |
| external | `yen` | Yen Pham | yen@partner-b.example | External resolver (Partner B) |

## Admin Consoles

Login with `admin` / `admin`:
- **Central Broker (`kc-central`)**: [http://localhost:8080/admin](http://localhost:8080/admin)
- **Internal Realm (`kc-internal`)**: [http://localhost:8081/admin](http://localhost:8081/admin)
- **External Realm (`kc-external`)**: [http://localhost:8082/admin](http://localhost:8082/admin)

## Acceptance Criteria & Demo Script

| # | Arch | Steps | Expected |
| --- | --- | --- | --- |
| 1 | – | Open `http://localhost:3000/` | Home screen with two scenario cards. |
| 2 | 1 | Navigate to `/architecture-1` → Click "Sign in" | Blue central login page with exactly two buttons (Staff, Partner), no password field. |
| 3 | 1 | Click "Staff" → green internal page → Login as `alice` / `password` | Back on `/architecture-1`; badge `INTERNAL`; decided by `roles claim`; `roles` contains `internal_user`; `idp` = `internal`; sees T-1…T-4 including internal comments. |
| 4 | 1 | Sign out → Sign in → Click "Partner" → orange external page → Login as `xavier` / `password` | Badge `EXTERNAL`; `roles` contains `external_resolver`; sees only T-2 with shared comment; "Add internal note" returns 403. |
| 5 | 1 | In central admin console delete mapper `external-gets-external_resolver` and remove role from user `xavier`, sign in again as `xavier` | Rejected with `?error=no_role`; no session created (fail closed). Restore by resetting containers. |
| 6 | 2 | Navigate to `/architecture-2` | Two buttons: "Sign in as internal", "Sign in as external". |
| 7 | 2 | Click "Sign in as internal" → green page → Login as `bob` / `password` | Badge `INTERNAL`; decided by `login route + issuer`; login route `/auth/a2/internal/callback`; `iss` = `http://localhost:8081/realms/internal`; no `roles` claim needed. |
| 8 | 2 | Sign out → Click "Sign in as external" → orange page → Login as `yen` / `password` | Badge `EXTERNAL`; sees only T-3; internal note returns 403. |
| 9 | 2 | Click "Sign in as external", enter `alice` / `password` | Login fails on the external page (alice does not exist there): wrong button cannot make a staff user external or vice versa. |
| 10 | 2 | Start "Sign in as internal", then in browser address bar replace `/internal/callback` with `/external/callback` on return URL | Returns 400 "no login in progress for this provider"; no session created. |
| 11 | 1+2 | Sign in as `alice` in both scenarios (two tabs) | Both sessions coexist; user key (`iss\|sub`) differs between broker and direct realm. |

## Smoke Test

Run the non-interactive automated smoke check:
```bash
node scripts/smoke.mjs
```

## Reset

To stop containers and wipe all state:
```bash
docker compose down
```
