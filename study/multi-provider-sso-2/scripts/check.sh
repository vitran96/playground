#!/usr/bin/env bash
# Automated security and isolation acceptance checks for Keycloak Login Architectures
# Run with: bash scripts/check.sh

set -euo pipefail

echo "=========================================================="
echo "  Running Keycloak Login Architectures Acceptance Checks  "
echo "=========================================================="

# 1. Backend is not reachable from the host.
echo -n "[1/5] Checking backend is NOT accessible directly from host (:3000)... "
if curl -s -m 2 http://localhost:3000/api/health >/dev/null 2>&1; then
  echo "FAILED! Backend port 3000 is directly reachable on host."
  exit 1
else
  echo "OK (connection refused/timed out as expected)"
fi

# 2. Forged identity headers are stripped: we are sent to Keycloak, not logged in.
echo -n "[2/5] Checking forged identity headers are stripped by gateway... "
REDIRECT_URL=$(curl -s -o /dev/null -w '%{redirect_url}' \
  -H 'X-RemoteUser: alice' -H 'X-RemoteUserRole: app-admin' \
  -H 'X-Gateway-Id: gw-1' -H 'X-Gateway-Secret: guess' \
  http://localhost:8101/api/architecture-1/login)

if [[ "${REDIRECT_URL}" =~ localhost:8082/realms/external ]]; then
  echo "OK (redirected to Keycloak-2 OIDC auth endpoint)"
else
  echo "FAILED! Expected redirect to Keycloak-2, got: '${REDIRECT_URL}'"
  exit 1
fi

# 3. A gateway cannot reach another architecture's API.
echo -n "[3/5] Checking architecture isolation across gateways (gw-2 -> arch-1)... "
HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8201/api/architecture-1/me)
if [ "${HTTP_CODE}" = "404" ]; then
  echo "OK (HTTP 404)"
else
  echo "FAILED! Expected HTTP 404, got: ${HTTP_CODE}"
  exit 1
fi

# 4. Arch 4 local endpoints refuse other gateways.
echo -n "[4/5] Checking Arch-4 local auth endpoint rejects internal gateway (gw-4-int)... "
HTTP_LOCAL=$(curl -s -o /dev/null -w '%{http_code}' -X POST http://int.localhost:8401/api/architecture-4/local/login)
if [ "${HTTP_LOCAL}" = "403" ] || [ "${HTTP_LOCAL}" = "404" ]; then
  echo "OK (HTTP ${HTTP_LOCAL})"
else
  echo "FAILED! Expected 403 or 404, got: ${HTTP_LOCAL}"
  exit 1
fi

# 5. Token replay check instructions
echo "[5/5] Token Replay Check across zones (manual check):"
echo "      Log into ext.localhost:8302/architecture-3/ as carol."
echo "      Copy the value of the 'architecture-3-external-session' cookie."
echo "      Run:"
echo "        curl -s -o /dev/null -w '%{http_code}\n' \\"
echo "          -H 'Cookie: architecture-3-internal-session=<EXT_TOKEN>' \\"
echo "          http://int.localhost:8301/api/architecture-3/me"
echo "      Expected output: 401 (rejected due to audience mismatch architecture-3:external != architecture-3:internal)"

echo "=========================================================="
echo "  All automated security checks completed successfully!   "
echo "=========================================================="
