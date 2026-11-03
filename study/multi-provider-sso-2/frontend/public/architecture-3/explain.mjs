/**
 * Architecture 3 details and trade-offs.
 * Used by: Architecture 3 frontend and documentation.
 */

export const explain = {
  title: 'Architecture 3: Two Domains, Two Gateways (Network-Decided Routing)',
  summary:
    'Two entry points. The internal domain goes straight to keycloak-1, so domain PCs get true zero-click login. The external domain uses keycloak-2.',
  diagram: `flowchart LR
    subgraph INT["Internal domain: int.localhost:8401"]
      BI["Employee browser"] --> GI["gw-3-int"]
    end
    subgraph EXT["External domain: ext.localhost:8402"]
      BX["Partner browser"] --> GX["gw-3-ext"]
    end
    GI -- OIDC --> K1["keycloak-1<br/>SPNEGO / password"]
    GX -- OIDC --> K2["keycloak-2<br/>email + password"]
    GI -- "X-Gateway-Id: gw-3-int" --> BE["Backend"]
    GX -- "X-Gateway-Id: gw-3-ext" --> BE
    BE -. "architecture-3-internal-session<br/>aud architecture-3:internal" .-> BI
    BE -. "architecture-3-external-session<br/>aud architecture-3:external" .-> BX`,
  flow: [
    'Internal Flow (http://int.localhost:8401):',
    '1. User navigates to http://int.localhost:8401/architecture-3/.',
    '2. gw-3-int protects /login, initiating OIDC directly against keycloak-1.',
    '3. keycloak-1 validates credentials silently via SPNEGO/Kerberos on AD domain (password form locally).',
    '4. gw-3-int proxies to backend with X-Gateway-Id: gw-3-int, X-RemoteUser, and X-RemoteUserRole.',
    '5. Backend issues cookie architecture-3-internal-session with audience architecture-3:internal.',
    'External Flow (http://ext.localhost:8402):',
    '1. User navigates to http://ext.localhost:8402/architecture-3/.',
    '2. gw-3-ext protects /login, initiating OIDC against keycloak-2.',
    '3. keycloak-2 handles external authentication (email/password or federated IdPs).',
    '4. gw-3-ext proxies to backend with X-Gateway-Id: gw-3-ext.',
    '5. Backend issues cookie architecture-3-external-session with audience architecture-3:external.',
  ],
  // Answers to shared CHECKS: auto-login, internal KC exposed, manual login exposed, stolen JWT reusable, KC manages external, registration in app, reset in app (null = N/A)
  checklist: [true, false, false, false, true, false, false],
  status: 'review', // 'none' | 'review' | { skip: '<reason>' }
  notes: [
    'Stolen-JWT replay across zones is avoided: each zone\'s gateway sets a hidden header for the backend (X-Gateway-Id, stripped from client requests and proven with X-Gateway-Secret) telling it whether the request is internal or external. The backend binds the JWT audience to that zone, so an internal JWT replayed via the external domain is rejected (401).',
    'keycloak-1 (SPNEGO + manual password login) stays internal-only and is never published to the internet, so there is no exposure risk. Only keycloak-2 is internet-facing (contrast with Architecture 5).',
    'Tokens are cryptographically bound to the entry point: a token minted on the internal side is rejected on the external side (aud check), even if copied into the external cookie.',
    'Adding external users directly requires a keycloak-2 service account.',
    'In production, the internal hostname is published via internal DNS or Zscaler ZPA application segments.',
    'The two gateways (gw-3-int / gw-3-ext) can be combined into one: one Apache with a VirtualHost per domain name, each setting the network-zone header (X-Gateway-Id) and its OIDC provider (keycloak-1 / keycloak-2). Caveat: the zone is then trusted from the Host header, so the internet-facing listener must only serve the external domain (otherwise an outsider can send Host: int.* and reach keycloak-1).',
  ],
  pros: [
    'Achieves true zero-click auto-login for domain users on the internal hostname.',
    'No Keycloak-level routing or guessing required.',
    'Strongest separation of internal and external traffic.',
  ],
  cons: [
    'Two distinct hostnames/URLs to communicate to users.',
    'Shared links across colleagues may point to the wrong side.',
    'Laptops roaming between office and remote networks may switch entry points and require re-login.',
    'Requires network team coordination (split DNS / Zscaler ZPA).',
  ],
  tryThis: [
    'Open int.localhost:8401/architecture-3/ as alice and ext.localhost:8402/architecture-3/ as carol in the same browser; observe two isolated cookies.',
    'Execute the token replay test in scripts/check.sh: injecting the external token into the internal domain returns 401.',
  ],
}
