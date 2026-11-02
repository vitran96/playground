# Implementation Verification & Deviations Log

This document records version-specific behaviors, schema verifications, and minor environment adjustments made relative to the initial plan specifications.

## 1. Pinned Component Versions

| Component | Image / Package | Pinned Version | Notes |
|---|---|---|---|
| Keycloak | `quay.io/keycloak/keycloak` | `26.8.1` | Stable release |
| Apache Gateway | `docker.io/library/debian:bookworm-slim` | `debian:bookworm-slim` | `apache2 2.4.68`, `libapache2-mod-auth-openidc 2.4.12.3` |
| Frontend | `docker.io/library/nginx:1.27-alpine` | `1.27-alpine` | Static asset serving |
| Backend | `docker.io/library/node:22-alpine` | `22-alpine` | Hono 4.6.18, Node 22 |
| Mailpit | `docker.io/axllent/mailpit:v1.33.0` | `v1.33.0` | SMTP testing on :8025 / :1025 |

---

## 2. Verified Technical Details

### A. Keycloak 26.8.1 Organizations Schema & kcadm Configuration
- **Domain Auto-Redirect:** In Keycloak 26.8.1, the Organization domain resource includes fields `verified` and `autoRedirect`:
  ```json
  "domains": [
    {
      "name": "internal.email.com",
      "verified": true,
      "autoRedirect": true
    }
  ]
  ```
  This is configured directly during organization creation in `setup-organization.sh`.
- **Linking Identity Provider to Organization:**
  Calling `kcadm.sh create organizations/<id>/identity-providers` in Keycloak 26.8.1 requires passing the provider alias as a JSON string literal (`-b '"internal-kc1"'`). Verified and incorporated into `keycloak/kc2/init/setup-organization.sh`.

### B. Apache mod_auth_openidc Directive Verification
- Tested with `apache2ctl -t` on Debian Bookworm:
  - `OIDCStateCookiePrefix` is fully supported.
  - `OIDCPathAuthRequestParams kc_idp_hint=internal-kc1` inside `<Location /api/architecture-2/login/internal>` validates cleanly.
  - `RequestHeader set X-RemoteUser "expr=%{REMOTE_USER}"` and `RequestHeader set X-RemoteUserRole "expr=%{reqenv:OIDC_CLAIM_roles}"` execute without syntax errors.

### C. Podman & SELinux Compatibility
- On Fedora/RHEL hosts running rootless Podman with SELinux enabled, bind-mount volumes require the `:z` private mount flag. This was added across `compose.yaml` volume declarations.
- Fully qualified image references (`docker.io/library/...`) were specified in Dockerfiles to prevent unqualified registry prompts during build.
