# Active Directory / Kerberos Secrets

This directory contains configuration and keytabs for the real Active Directory demo (`compose.ad.yaml`).

## Files
- `krb5.conf.example`: Template for Kerberos KDC resolution. Copy to `krb5.conf`.
- `keycloak.keytab`: Generated via `ktpass` on the domain controller. **Must be file mode 600 and kept secret.**

Both `krb5.conf` and `*.keytab` are git-ignored.
See `docs/keycloak-ad-setup.md` for complete runbook instructions.
