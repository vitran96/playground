# GitHub Copilot Instructions

## Workspace Architecture
This repository is an OIDC Multi-Provider Single Sign-On benchmark containing 7 complete reference architectures across Keycloak realms, Traefik reverse proxies, and a unified Hono backend.

## Conventions & Rules
- **Modules**: Native Node.js ESM (`import`/`export`), no CommonJS.
- **Frontend**: Preact via `htm` tagged template literals in `app/public/app.js`. Do not import or bundle React/Webpack/Vite.
- **Routing & Endpoints**:
  - Arch 1: `/architecture-1`, API `/api/a1/*`, Auth `/auth/a1/*`
  - Arch 2: `/architecture-2`, API `/api/a2/*`, Auth `/auth/a2/*`
  - Arch 3: `/architecture-3`, API `/api/a3/*`, Auth `/auth/a3/*`
  - Arch 4: `/architecture-4`, API `/api/a4/*`, Auth `/auth/a4/*`
  - Arch 5: `/architecture-5` & `/architecture-5/internal`, API `/api/a5/*` & `/api/a5/internal/*`
  - Arch 6: `/architecture-6`, API `/api/a6/*`, Auth `/auth/a6/*`
  - Arch 7: `/architecture-7`, API `/api/a7/*`, Auth `/auth/a7/*`
- **Security**: Maintain PKCE S256 verification and fail-closed classification.
- **Theme**: Light mode (`data-theme="light"`), primary `#393939`, secondary `#7E6C39`, body white.

Consult `INSTRUCTIONS.md` and `architecture-[1-7].instruction.md` for individual architecture specs.
