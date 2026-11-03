// @ts-nocheck
/**
 * Architecture 4 details and trade-offs.
 * Used by: Architecture 4 frontend and documentation.
 */

export const explain = {
  title: 'Architecture 4: Two Domains with App-Owned External Login',
  summary:
    'Internal users get zero-click Keycloak login; external users never see Keycloak, the app manages their accounts.',
  diagram: `flowchart LR
    subgraph INT["Internal domain: int.localhost:8501"]
      BI["Employee browser"] --> GI["gw-4-int"]
    end
    subgraph EXT["External domain: ext.localhost:8502"]
      BX["Partner browser<br/>app-owned login form"] --> GX["gw-4-ext<br/>plain reverse proxy, no OIDC"]
    end
    GI -- OIDC --> K1["keycloak-1<br/>SPNEGO / password"]
    GI -- "X-Gateway-Id: gw-4-int" --> BE["Backend"]
    GX -- "POST /local/login, signup, forgot/reset" --> BE
    BE --- US[("Local user store<br/>scrypt hashes")]
    BE -. "architecture-4-internal-session<br/>aud architecture-4:internal" .-> BI
    BE -. "architecture-4-external-session<br/>aud architecture-4:external" .-> BX`,
  flow: [
    'Internal Flow (http://int.localhost:8501):',
    '1. Identical to Architecture 3: gw-4-int proxies to keycloak-1 for zero-click SPNEGO on domain PCs.',
    '2. Sets cookie architecture-4-internal-session with audience architecture-4:internal.',
    'External Flow (http://ext.localhost:8502):',
    '1. gw-4-ext has no OIDC module configured; functions as a pure reverse proxy.',
    '2. Frontend detects loginUi="form" from GET /api/architecture-4/gateway and renders local auth tabs.',
    '3. Sign-in: POST /api/architecture-4/local/login validates password against in-memory scrypt hashes.',
    '4. Backend issues cookie architecture-4-external-session with audience architecture-4:external.',
    '5. Forgot password: POST /api/architecture-4/local/forgot-password emails a 6-digit code (10 min, 5 tries) via Mailpit; POST /local/reset-password with email + code sets the new password.',
  ],
  // Answers to shared CHECKS: auto-login, internal KC exposed, manual login exposed, stolen JWT reusable, KC manages external, registration in app, reset in app (null = N/A)
  checklist: [true, false, false, false, false, true, true],
  status: 'review', // 'none' | 'review' | { skip: '<reason>' }
  notes: [
    'Stolen-JWT replay across zones is avoided: each zone\'s gateway sets a hidden header for the backend (X-Gateway-Id, stripped from client requests and proven with X-Gateway-Secret) telling it whether the request is internal or external. The backend binds the JWT audience to that zone, so an internal JWT replayed via the external domain is rejected (401).',
    'keycloak-1 (SPNEGO + manual password login) stays internal-only and is never published to the internet, so there is no exposure risk. No Keycloak is internet-facing (contrast with Architecture 5).',
    'Recommended: move authentication into Keycloak instead of the app calling the internal "integration service" to verify logins. An app-owned, internet-facing login endpoint that relays credentials to an internal service exposes that service to dictionary / credential-stuffing attacks; Keycloak provides brute-force detection, lockout, and MFA out of the box.',
    'Sign-up no longer depends on Keycloak: the app owns the external user store completely.',
    'The engineering team owns external credential security: hashing, reset flows, lockout/rate limiting, MFA, and compliance.',
    'Adding federated external OIDC providers later requires building OIDC handling inside the app or re-introducing an external IdP.',
    'The two gateways (gw-4-int / gw-4-ext) can be combined into one: one Apache with a VirtualHost per domain name, each setting the network-zone header (X-Gateway-Id); only the internal VirtualHost enables OIDC (keycloak-1). Caveat: the zone is then trusted from the Host header, so the internet-facing listener must only serve the external domain (otherwise an outsider can send Host: int.* and reach keycloak-1).',
  ],
  pros: [
    'Eliminates keycloak-2: one fewer IdP cluster to maintain and patch.',
    'Full control of the external login, registration, and onboarding UX.',
    'Internal users retain true zero-click login.',
  ],
  cons: [
    'Critical security logic (password hashing, reset tokens, auth endpoints) shifts to the application.',
    'Two distinct identity paradigms to maintain.',
    'Retains all two-domain trade-offs (DNS complexity, disjoint bookmark URLs).',
  ],
  tryThis: [
    'Sign up a new external partner on ext.localhost:8502/architecture-4/.',
    'Use "Forgot password", read the 6-digit code in Mailpit (http://localhost:8125), reset password, and log in.',
    'Notice that attempting to hit /api/architecture-4/local/login from the internal gateway (int.localhost:8501) returns 403.',
  ],
}
