# Active Directory, Kerberos (SPNEGO), and LDAP Integration Runbook

This runbook guides infrastructure and Active Directory administrators in configuring `keycloak-1` for seamless SPNEGO/Kerberos auto-login on AD domain-joined workstations, with password fallback.

## 1. Authentication Flow on Domain Workstations

1. **SPNEGO (Zero-Click):**
   - The browser navigates to the application gateway.
   - Gateway redirects unauthenticated requests to `keycloak-1`.
   - `keycloak-1` responds with `401 Unauthorized` containing header `WWW-Authenticate: Negotiate`.
   - Workstation requests a service ticket for `HTTP/sso.corp.example.com` from the Active Directory Key Distribution Center (KDC).
   - Browser sends the ticket in `Authorization: Negotiate <ticket>`.
   - `keycloak-1` validates the ticket using its keytab, completes authentication, and redirects to the gateway callback without presenting any UI.
2. **Kerberos Password Fallback:**
   - If SPNEGO fails (off-network, untrusted browser, VPN without line-of-sight to DC), Keycloak renders its login form.
   - When configured, Keycloak validates the user credentials directly against the KDC.
3. **LDAP Bind Fallback:**
   - Alternatively, Keycloak binds against Active Directory over LDAPS using the supplied credentials.

---

## 2. Prerequisites

- Dedicated FQDN with an **A record** (e.g. `sso.corp.example.com`). Do not use CNAMEs, IP addresses, or localhost.
- Valid TLS certificate configured on the reverse proxy or Keycloak.
- NTP clock synchronization within 5 minutes of Domain Controllers.
- Network connectivity from Keycloak to DCs on:
  - Port 88 TCP/UDP (Kerberos)
  - Port 636 TCP (LDAPS)

---

## 3. Step 1: Active Directory Service Account, SPN, and Keytab

Run the following commands in PowerShell on a Domain Controller as an AD Administrator:

```powershell
# 1. Create a dedicated service account (non-privileged)
New-ADUser svc-keycloak -AccountPassword (Read-Host -AsSecureString) -Enabled $true `
  -PasswordNeverExpires $true -KerberosEncryptionType AES256

# 2. Register Service Principal Name (SPN)
setspn -S HTTP/sso.corp.example.com svc-keycloak

# 3. Verify no duplicate SPNs exist
setspn -X

# 4. Export Kerberos Keytab (AES256)
# NOTE: ktpass /pass resets the account password; rerunning it invalidates old keytabs.
ktpass /princ HTTP/sso.corp.example.com@CORP.EXAMPLE.COM /mapuser CORP\svc-keycloak `
  /crypto AES256-SHA1 /ptype KRB5_NT_PRINCIPAL /pass * /out keycloak.keytab
```

Transfer `keycloak.keytab` securely to `keycloak/ad/keycloak.keytab` with file permissions `600` (`chmod 600 keycloak/ad/keycloak.keytab`).

---

## 4. Step 2: Kerberos Configuration & Docker Override

Create `keycloak/ad/krb5.conf`:

```ini
[libdefaults]
  default_realm = CORP.EXAMPLE.COM
  dns_lookup_kdc = true
  rdns = false
  default_tkt_enctypes = aes256-cts-hmac-sha1-96
  default_tgs_enctypes = aes256-cts-hmac-sha1-96

[realms]
  CORP.EXAMPLE.COM = {
    kdc = dc1.corp.example.com
    kdc = dc2.corp.example.com
  }

[domain_realm]
  .corp.example.com = CORP.EXAMPLE.COM
```

Launch Keycloak using the AD override:

```bash
docker compose -f compose.yaml -f compose.ad.yaml up -d keycloak-1
```

---

## 5. Step 3: User Federation in Realm `internal`

In Keycloak Admin Console (`https://sso.corp.example.com/admin`):

1. Navigate to **User Federation** -> **Add LDAP Provider**.
2. Configure settings:
   - **Vendor:** Active Directory
   - **Connection URL:** `ldaps://dc1.corp.example.com:636`
   - **Bind DN:** `CN=svc-keycloak-ldap,OU=Service Accounts,DC=corp,DC=example,DC=com`
   - **Bind Credential:** `<password>`
   - **Edit Mode:** `READ_ONLY`
   - **Users DN:** `OU=Staff,DC=corp,DC=example,DC=com`
   - **Username LDAP Attribute:** `sAMAccountName`
   - **RDN Attribute / UUID Attribute:** `cn` / `objectGUID`
   - **User Object Classes:** `person, organizationalPerson, user`
   - **Allow Kerberos Authentication:** `ON`
   - **Kerberos Realm:** `CORP.EXAMPLE.COM`
   - **Server Principal:** `HTTP/sso.corp.example.com@CORP.EXAMPLE.COM`
   - **Key Tab:** `/opt/keycloak/conf/keycloak.keytab`
   - **Kerberos Principal Attribute:** `userPrincipalName`
   - **Use Kerberos for Password Authentication:** `ON`
3. Add a **group-ldap-mapper** to map AD groups to realm roles:
   - `APP-Employees` -> `employee`
   - `APP-Admins` -> `app-admin`

---

## 6. Step 4: Authentication Browser Flow

In Keycloak Admin Console:
1. Go to **Authentication** -> **Flows** -> **Browser**.
2. Set **Kerberos** to `Alternative` (default is `Disabled`).
3. Set **Cookie** to `Alternative`.
4. Set **Forms** to `Alternative`.

---

## 7. Step 5: Client Browser Policy (GPO)

Configure Group Policy Objects (GPO) for enterprise workstations:
- **Chrome / Edge:** Add `https://sso.corp.example.com` to `AuthServerAllowlist` or the Local Intranet zone.
- **Firefox:** Set `network.negotiate-auth.trusted-uris` to `https://sso.corp.example.com`.
- **Zscaler / Proxy:** Exclude `sso.corp.example.com` from SSL inspection and ensure `Authorization` / `WWW-Authenticate` headers are preserved without stripping.

---

## 8. Verification & Troubleshooting

Test Kerberos ticket acquisition from a workstation:
```bash
# Windows
klist get HTTP/sso.corp.example.com

# Linux
kinit user@CORP.EXAMPLE.COM
curl -sk --negotiate -u : -o /dev/null -w '%{http_code}\n' \
  "https://sso.corp.example.com/realms/internal/protocol/openid-connect/auth?client_id=app-arch3-int&response_type=code&scope=openid&redirect_uri=http://int.localhost:8401/api/architecture-3/login/callback"
# Expect: 302 redirect with code
```

Common issues:
- **Form always shown:** Hostname not in Intranet zone; Kerberos execution disabled in Browser flow.
- **`KRB_AP_ERR_MODIFIED`:** Keytab out of date; re-export with `ktpass` and ensure password was not changed afterwards.
- **Works direct, fails via proxy:** Network proxy stripping `Negotiate` headers.
