# Claude Code Guidelines

## Project Context
Multi-Provider OIDC SSO Architecture Demo implementing 7 distinct identity federation patterns with Keycloak, Traefik, and Node.js/Hono.

## Key Directives
- **Zero-Build Architecture**: Never add bundlers, transpilers, or build scripts. Keep frontend in vanilla ES modules with `htm/preact`.
- **Fail Closed**: In security checks, reject authentication whenever role or state cannot be verified.
- **Pico CSS v2 Light Theme**: Keep `data-theme="light"`, primary `#393939`, secondary `#7E6C39`, body white `#ffffff`.
- **Primary Reference**: Detailed architecture specifications are in `INSTRUCTIONS.md` and `architecture-[1-7].instruction.md`.

## Common Commands
- Run backend locally: `cd app && npm start`
- Run smoke test suite: `node scripts/smoke.mjs`
- Validate syntax: `node -c app/public/app.js app/src/*.js`
- Docker compose: `docker compose up --build` / `docker compose down -v`

## Test Accounts (Password: `password`)
- Internal Staff: `alice` (internal triage), `bob` (internal resolver)
- External Partner: `xavier` (Partner A), `yen` (Partner B)
