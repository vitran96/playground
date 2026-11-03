#!/usr/bin/env bash
# One-shot idempotent flow setup for an external Keycloak realm (keycloak-2/3/4).
# Binds custom flows provided by the demo SPI (keycloak/spi):
#  - "registration external": built-in registration + external-registration (deny company domain, grant partner)
#  - "reset credentials email otp": choose user -> email 6-digit code -> set new password

set -euo pipefail

KCADM="/opt/keycloak/bin/kcadm.sh"
SERVER="${KC_SERVER:-http://keycloak-2:8180}"
REALM="${KC_REALM:-external}"
ADMIN_USER="${KC_BOOTSTRAP_ADMIN_USERNAME:-admin}"
ADMIN_PASS="${KC_BOOTSTRAP_ADMIN_PASSWORD:-admin}"

for i in {1..60}; do
  ${KCADM} config credentials --server "${SERVER}" --realm master --user "${ADMIN_USER}" --password "${ADMIN_PASS}" 2>/dev/null && break
  sleep 2
done

flow_exists() { ${KCADM} get authentication/flows -r "${REALM}" --fields alias | grep -q "\"$1\""; }
new_flow() { ${KCADM} create authentication/flows -r "${REALM}" -s alias="$1" -s providerId="$2" -s topLevel="$3" -s builtIn=false -i; }
# add_exec <parentFlowId> <authenticator> [subFlowId]; executions are appended in call order
add_exec() {
  local args=(-s parentFlow="$1" -s authenticator="$2" -s requirement=REQUIRED -s priority="${PRIO}")
  [ -n "${3:-}" ] && args+=(-s authenticatorFlow=true -s flowId="$3")
  ${KCADM} create authentication/executions -r "${REALM}" "${args[@]}"
  PRIO=$((PRIO + 10))
}

if ! flow_exists "registration external"; then
  echo "==> [${REALM}] creating 'registration external' flow"
  PRIO=10
  TOP=$(new_flow "registration external" basic-flow true)
  FORM=$(new_flow "registration external form" form-flow false)
  add_exec "${TOP}" registration-page-form "${FORM}"
  add_exec "${FORM}" registration-user-creation
  add_exec "${FORM}" external-registration
  add_exec "${FORM}" registration-password-action
fi

if ! flow_exists "reset credentials email otp"; then
  echo "==> [${REALM}] creating 'reset credentials email otp' flow"
  PRIO=10
  TOP=$(new_flow "reset credentials email otp" basic-flow true)
  add_exec "${TOP}" reset-credentials-choose-user
  add_exec "${TOP}" email-otp-reset
  add_exec "${TOP}" reset-password
fi

${KCADM} update "realms/${REALM}" -s registrationFlow="registration external" -s resetCredentialsFlow="reset credentials email otp"
echo "==> [${REALM}] flows bound."
