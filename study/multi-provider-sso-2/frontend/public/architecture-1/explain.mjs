// @ts-nocheck
/**
 * Architecture 1 details and trade-offs.
 * Used by: Architecture 1 frontend and documentation.
 */

export const explain = {
  title: 'Architecture 1: Single Gateway with Email-First Identity Routing',
  summary:
    'One URL for everyone. keycloak-3 (dedicated to this architecture) asks for an email first; company emails go to keycloak-1, everyone else types a password.',
  diagram: `sequenceDiagram
    participant B as Browser
    participant GW as gw-1 (Apache)
    participant K3 as keycloak-3
    participant K1 as keycloak-1
    participant BE as Backend
    B->>GW: GET /api/architecture-1/login
    GW-->>B: 302 to keycloak-3 (PKCE)
    B->>K3: Enter email
    alt email ends with @internal.email.com
      K3-->>B: 302 to keycloak-1 (Organization domain match)
      B->>K1: password / SPNEGO
      K1-->>B: 302 code to keycloak-3 broker
      B->>K3: broker callback
      K3->>K1: code exchange (backchannel)
    else any other email
      B->>K3: password
    end
    K3-->>B: 302 code to gw-1 callback
    B->>GW: callback
    GW->>K3: code exchange, validate ID token
    GW->>BE: /login + X-RemoteUser, X-RemoteUserRole, X-RemoteUserIdp
    BE-->>B: Set-Cookie architecture-1-session (aud architecture-1:any), 302 /architecture-1/`,
  flow: [
    'Browser loads /architecture-1/, calls GET /api/architecture-1/me, receives 401.',
    'Browser navigates to GET /api/architecture-1/login. Gateway (gw-1) checks session, finds none, and redirects (302) with PKCE to keycloak-3.',
    "keycloak-3 displays its identity-first prompt: 'Email?'.",
    'User enters company email (@internal.email.com); keycloak-3 Organization matches domain and redirects (302) to internal-kc1 IdP (keycloak-1).',
    'keycloak-1 authenticates the user (password locally, SPNEGO/Kerberos in domain environment).',
    'keycloak-1 returns authorization code to keycloak-3 broker endpoint.',
    'keycloak-3 exchanges code with keycloak-1 backchannel, mints its own token, and returns code to gw-1 redirect URI.',
    'gw-1 completes code exchange, validates ID token, strips incoming headers, sets X-RemoteUser, X-RemoteUserRole, and X-RemoteUserIdp, then proxies /login to backend.',
    'Backend verifies gw-1 secret, sets HttpOnly JWT session cookie (architecture-1-session), and redirects (302) to /architecture-1/.',
    'If user entered an external email (e.g. carol@partner.example.com), keycloak-3 prompts for password directly without contacting keycloak-1.',
  ],
  // Answers to shared CHECKS: auto-login, internal KC exposed, manual login exposed, stolen JWT reusable, KC manages external, registration in app, reset in app (null = N/A)
  checklist: [false, false, false, true, true, false, false],
  status: { skip: 'similar to Arch 2; requires manual interaction before internal SSO login' }, // 'none' | 'review' | { skip: '<reason>' }
  notes: [
    'Single domain risk: internal and external users share one entry point and one token audience (architecture-1:any), so if an external user somehow steals an internal user\'s app JWT they can replay it and log in as that internal user. The backend cannot tell which side the request came from.',
    'keycloak-1 (SPNEGO + manual password login) stays internal-only and is never published to the internet, so there is no exposure risk. Only keycloak-3 is internet-facing (contrast with Architecture 5).',
    'Adding external users directly from the app requires a keycloak-3 service account (confidential client with realm-management -> manage-users).',
    'Enables adding additional external OIDC identity providers later (e.g. Entra ID, Google) on keycloak-3 with zero app or gateway changes.',
    'Self-service password reset and user registration are managed by Keycloak (emails captured in Mailpit).',
    'Future optimization: App could store an "employee" cookie preference and send kc_idp_hint to bypass the email step on return visits.',
  ],
  pros: [
    'Single entry URL and single OIDC client configuration.',
    'Internal users enter only their email address once per session.',
    'No network-level routing or split DNS required; functions identically at home, office, and Zscaler.',
  ],
  cons: [
    'Not zero-click on initial visit (requires email entry).',
    'Depends on Keycloak Organizations feature (configuration schema is version-sensitive).',
    'Employee browsers must have network reachability to keycloak-1.',
  ],
  tryThis: [
    'Log in as alice@internal.email.com (password: alice): observe redirect to keycloak-1, session shows user_type: internal, roles: employee, app-admin.',
    'Log in as carol@partner.example.com (password: carol): stays on keycloak-3, session shows user_type: external, role: partner.',
    'Verify that session card displays audience architecture-1:any.',
  ],
}
