#!/usr/bin/env bash
# One-shot idempotent initialization script for a broker Keycloak (default: keycloak-2, realm 'external').
# Reused for keycloak-3 (realm 'arch1') via KC_SERVER / KC_REALM.
# Configures the 'internal-corp' organization and links identity provider 'internal-kc1'
# with email-domain auto-redirection.

set -euo pipefail

KCADM="/opt/keycloak/bin/kcadm.sh"
SERVER="${KC_SERVER:-http://keycloak-2:8180}"
REALM="${KC_REALM:-external}"
ADMIN_USER="${KC_BOOTSTRAP_ADMIN_USERNAME:-admin}"
ADMIN_PASS="${KC_BOOTSTRAP_ADMIN_PASSWORD:-admin}"

echo "==> Waiting for Keycloak to be ready at ${SERVER}..."
for i in {1..60}; do
  if ${KCADM} config credentials --server "${SERVER}" --realm master --user "${ADMIN_USER}" --password "${ADMIN_PASS}" 2>/dev/null; then
    echo "==> Successfully authenticated against Keycloak Admin REST API (${SERVER})."
    break
  fi
  sleep 2
done

echo "==> Configuring organization 'internal-corp' in realm '${REALM}'..."

# Check if organization already exists
ORG_ID=$(${KCADM} get organizations -r "${REALM}" 2>/dev/null | grep -B 2 '"alias" : "internal-corp"' | grep '"id"' | head -n1 | tr -d '",: ' | sed 's/id//' || true)

if [ -z "${ORG_ID}" ]; then
  echo "==> Creating organization 'internal-corp' with domain 'internal.email.com'..."
  ORG_OUTPUT=$(${KCADM} create organizations -r "${REALM}" \
    -s name="internal-corp" \
    -s alias="internal-corp" \
    -s 'domains=[{"name":"internal.email.com","verified":true}]' \
    -o)
  ORG_ID=$(echo "${ORG_OUTPUT}" | grep '"id"' | head -n1 | tr -d '",: ' | sed 's/id//')
  echo "==> Organization created with ID: ${ORG_ID}"
else
  echo "==> Organization 'internal-corp' exists with ID: ${ORG_ID}. Updating domain configuration..."
  ${KCADM} update "organizations/${ORG_ID}" -r "${REALM}" \
    -s 'domains=[{"name":"internal.email.com","verified":true}]' || true
fi

echo "==> Linking identity provider 'internal-kc1' to organization '${ORG_ID}'..."
# Keycloak 26.8+ accepts the alias as a JSON string body
${KCADM} create "organizations/${ORG_ID}/identity-providers" -r "${REALM}" -b '"internal-kc1"' 2>/dev/null || {
  echo "==> (IdP link already present or verified)"
}

# autoRedirect only fires when the domain names its IdP; the IdP must be linked first
echo "==> Binding domain 'internal.email.com' to 'internal-kc1' with auto-redirect..."
${KCADM} update "organizations/${ORG_ID}" -r "${REALM}" \
  -s name="internal-corp" -s alias="internal-corp" \
  -s 'domains=[{"name":"internal.email.com","verified":true,"autoRedirect":true,"identityProviderAlias":"internal-kc1"}]'

echo "==> Organization setup completed successfully."
exit 0
