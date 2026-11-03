// @ts-nocheck
/**
 * Architecture 5 details and trade-offs (evaluation only, not deployed in this demo).
 * Used by: Evaluation Portal "Show more" dialog.
 */

export const explain = {
  title: 'Architecture 5: Internal Keycloak Exposed to the Internet',
  summary:
    'One Keycloak for everyone: keycloak-1 (AD-backed, SPNEGO + password form) is published to the internet so employees and partners log in at the same public URL. Not deployed in this demo: evaluation only.',
  diagram: `flowchart LR
    subgraph NET["Internet"]
      BE1["Employee browser<br/>office / home"]
      BX["Partner browser"]
      ATK["Attacker / bots"]
    end
    BE1 --> GW["App gateway<br/>(public)"]
    BX --> GW
    GW -- OIDC --> K1["keycloak-1 (PUBLIC)<br/>SPNEGO + manual password login"]
    ATK -. "password spraying / dictionary<br/>Negotiate probing, CVE scans" .-> K1
    K1 -- "LDAP bind / Kerberos" --> AD[("Active Directory<br/>internal")]
    GW --> APP["Backend"]`,
  flow: [
    'Browser opens the public app URL; the gateway redirects (302) to keycloak-1 on its public hostname.',
    'keycloak-1 answers 401 WWW-Authenticate: Negotiate (SPNEGO) to every visitor on the internet.',
    'Domain-joined PCs with line-of-sight to a KDC send a Kerberos ticket; everyone else falls back to the keycloak-1 password form.',
    'The password form validates the credentials directly against Active Directory (LDAP bind).',
    'keycloak-1 returns the code to the gateway; the backend mints the app session as in the other architectures.',
  ],
  pros: [
    'Single Keycloak and single URL: no broker Keycloak, no Organizations, no split DNS.',
    'Simplest topology to operate and explain.',
    'Employees can log in from anywhere without VPN / Zscaler ZPA.',
  ],
  cons: [
    'RISK: the manual password form becomes an internet-facing oracle for AD credentials: password spraying, dictionary and credential-stuffing attacks hit real corporate accounts.',
    'RISK: repeated failures trigger AD account lockout, so attackers can lock out employees at will (denial of service).',
    'RISK: the SPNEGO / Kerberos endpoint is reachable from the internet: Negotiate handshakes (and any NTLM fallback) can leak AD domain / host names and add Kerberos parsing attack surface, while giving no benefit off-network (no KDC reachable).',
    'RISK: any Keycloak CVE or misconfiguration (admin console, account console, open endpoints) becomes directly exploitable from the internet and sits one hop from AD.',
    'Partners would be created inside the internal AD-backed realm, mixing employee and external identities.',
  ],
  // Answers to shared CHECKS: auto-login, internal KC exposed, manual login exposed, stolen JWT reusable, KC manages external, registration in app, reset in app (null = N/A)
  checklist: [true, true, true, true, true, null, null],
  status: { skip: 'not safe' }, // 'none' | 'review' | { skip: '<reason>' }
  notes: [
    'Single domain risk: everyone uses the same public entry point and token audience, so if an external user somehow steals an internal user\'s app JWT they can replay it and log in as that internal user. The backend cannot tell which side the request came from.',
    'Architectures 1-4 avoid the exposure by keeping keycloak-1 internal-only; only a dedicated external Keycloak (or the app) faces the internet.',
    'If chosen anyway, minimum mitigations: MFA for all users, Keycloak brute-force detection with Keycloak-side (not AD) lockout, WAF / rate limiting, block /admin on the public hostname, disable SPNEGO on the public hostname, patch Keycloak promptly.',
  ],
  tryThis: [
    'Not deployed in this demo: compare its risk profile with Architectures 1-4, where keycloak-1 is never exposed.',
  ],
}
