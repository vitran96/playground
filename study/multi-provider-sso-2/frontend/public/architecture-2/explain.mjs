// @ts-nocheck
/**
 * Architecture 2 details and trade-offs.
 * Used by: Architecture 2 frontend and documentation.
 */

export const explain = {
  title: 'Architecture 2: Single Gateway with User-Selected IdP Choice',
  summary:
    'The frontend asks employee or partner? and the gateway passes kc_idp_hint, so keycloak-4 (dedicated to this architecture) goes straight to the right login.',
  diagram: `sequenceDiagram
    participant B as Browser
    participant GW as gw-2 (Apache)
    participant K4 as keycloak-4
    participant K1 as keycloak-1
    participant BE as Backend
    B->>B: User picks Employee or Partner
    alt Employee
      B->>GW: GET /api/architecture-2/login/internal
      GW-->>B: 302 to keycloak-4 with kc_idp_hint=internal-kc1
      B->>K4: authorize (hint)
      K4-->>B: 302 straight to keycloak-1 (no keycloak-4 form)
      B->>K1: password / SPNEGO
      K1-->>B: 302 code to keycloak-4 broker
      K4->>K1: code exchange (backchannel)
    else Partner
      B->>GW: GET /api/architecture-2/login/external
      GW-->>B: 302 to keycloak-4 (no hint)
      B->>K4: email + password
    end
    K4-->>B: 302 code to gw-2 callback
    B->>GW: callback
    GW->>K4: code exchange
    GW->>BE: /login + X-RemoteUser, X-RemoteUserRole, X-RemoteUserIdp
    BE-->>B: Set-Cookie architecture-2-session (aud architecture-2:any)`,
  flow: [
    'Browser loads /architecture-2/, calls GET /api/architecture-2/me, receives 401.',
    'Frontend renders ChooseLogin UI with two buttons: "Sign in as Employee" and "Sign in as Partner".',
    'User clicks "Sign in as Employee" -> navigates to /api/architecture-2/login/internal.',
    'Gateway (gw-2) intercepts /login/internal and appends kc_idp_hint=internal-kc1 to the OIDC authorization request to keycloak-4.',
    'keycloak-4 immediately forwards the browser to keycloak-1, skipping its own login form.',
    'keycloak-1 authenticates the user (password locally, SPNEGO/Kerberos on AD domain).',
    'User is redirected back through keycloak-4 broker and gw-2 callback.',
    'gw-2 sets X-RemoteUser, X-RemoteUserRole, and X-RemoteUserIdp=internal-kc1, then proxies /login to backend.',
    'Backend validates gw-2 credentials, mints architecture-2-session JWT cookie (aud: architecture-2:any), redirects to /architecture-2/.',
    'If user clicks "Sign in as Partner" -> navigates to /api/architecture-2/login/external (no hint); keycloak-4 authenticates directly.',
  ],
  // Answers to shared CHECKS: auto-login, internal KC exposed, manual login exposed, stolen JWT reusable, KC manages external, registration in app, reset in app (null = N/A)
  checklist: [false, false, false, true, true, false, false],
  status: { skip: 'requires manual interaction before internal SSO login' }, // 'none' | 'review' | { skip: '<reason>' }
  notes: [
    'Single domain risk: internal and external users share one entry point and one token audience (architecture-2:any), so if an external user somehow steals an internal user\'s app JWT they can replay it and log in as that internal user. The backend cannot tell which side the request came from.',
    'keycloak-1 (SPNEGO + manual password login) stays internal-only and is never published to the internet, so there is no exposure risk. Only keycloak-4 is internet-facing (contrast with Architecture 5).',
    'Adding external users directly requires a keycloak-4 service account.',
    'Enables adding additional external identity providers on keycloak-4.',
    'Password reset and registration are handled via Keycloak.',
    'Organizations are disabled on keycloak-4 (realm arch2): the partner path shows a plain username/password form, routing is done by the button choice.',
  ],
  pros: [
    'Employees get a direct one-click path to keycloak-1 (zero-click on domain PCs).',
    'Straightforward implementation with mod_auth_openidc path-based parameter injection.',
    'Does not depend on email domain parsing for employee routing.',
  ],
  cons: [
    'Requires user to choose the correct button.',
    'If an off-network employee clicks Employee without line-of-sight to keycloak-1, connection fails.',
    'The choice is a UI routing mechanism, not a network security boundary.',
  ],
  tryThis: [
    'Click "Sign in as Employee" -> observe immediate redirect to keycloak-1 without keycloak-4 login form.',
    'On keycloak-1, try logging in with carol@partner.example.com (fails: carol exists only in the external Keycloaks).',
    'Click "Sign in as Partner" -> authenticate as carol on keycloak-4.',
  ],
}
