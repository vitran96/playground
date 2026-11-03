// @ts-nocheck
/**
 * Architecture 7 details and trade-offs.
 * Used by: Architecture 7 frontend and documentation.
 */

export const explain = {
  title: 'Architecture 7: Two Domains, Keycloak Login with App-Owned Registration & Reset',
  summary:
    'Architecture 6 plus an app-owned "Forgot password": the app emails a 6-digit code (same as Architecture 4) and sets the new password in keycloak-2 through the admin API. Sign-in still happens in keycloak-2.',
  diagram: `flowchart LR
    subgraph INT["Internal domain: int.localhost:8701"]
      BI["Employee browser"] --> GI["gw-7-int"]
    end
    subgraph EXT["External domain: ext.localhost:8702"]
      BX["Partner browser<br/>app login page"] --> GX["gw-7-ext"]
    end
    GI -- OIDC --> K1["keycloak-1<br/>SPNEGO / password"]
    GX -- "OIDC (Sign In)" --> K2["keycloak-2<br/>email + password"]
    GI -- "X-Gateway-Id: gw-7-int" --> BE["Backend"]
    GX -- "POST /self/register, forgot/reset" --> BE
    BE -- "admin API: create user,<br/>reset-password" --> K2
    BE -- "temporary password,<br/>6-digit code" --> MP[("Mailpit")]
    BE -. "architecture-7-internal-session<br/>aud architecture-7:internal" .-> BI
    BE -. "architecture-7-external-session<br/>aud architecture-7:external" .-> BX`,
  flow: [
    'Internal Flow (http://int.localhost:8701): unchanged from Architecture 3, gw-7-int goes straight to keycloak-1.',
    'External Flow (http://ext.localhost:8702):',
    '1. The app shows its own login page with Sign In, Register and Forgot Password buttons.',
    '2. Sign In: gw-7-ext runs OIDC against keycloak-2 (client app-arch7-ext).',
    '3. Register: same as Architecture 6 (app creates the keycloak-2 user, emails a temporary password).',
    '4. Forgot password: POST /api/architecture-7/self/forgot-password looks the user up in keycloak-2 and emails a 6-digit code (10 min, 5 tries, 1 per minute), same response whether or not the account exists.',
    '5. POST /api/architecture-7/self/reset-password verifies the code, then calls the keycloak-2 admin reset-password endpoint.',
    '6. Backend issues cookie architecture-7-external-session with audience architecture-7:external.',
  ],
  // Answers to shared CHECKS: auto-login, internal KC exposed, manual login exposed, stolen JWT reusable, KC manages external, registration in app, reset in app (null = N/A)
  checklist: [true, false, false, false, true, true, true],
  status: 'review', // 'none' | 'review' | { skip: '<reason>' }
  notes: [
    'Stolen-JWT replay across zones is avoided exactly as in Architecture 3 (X-Gateway-Id + per-zone JWT audience).',
    'Keycloak still owns credentials, login, brute-force detection and MFA; the app owns registration and reset UX.',
    'Reset codes live in backend memory (same store as Architecture 4, key-prefixed so codes never cross stores). Multiple backend replicas would need a shared store.',
    'The keycloak-2 Register and Forgot-password links are hidden for this client via the arch7-external login theme; the realm still allows both (shared with Architecture 3).',
    'Keycloak password policy is still enforced when the backend saves the new password through the admin API.',
  ],
  pros: [
    'Internal users keep true zero-click login.',
    'One consistent app-branded flow for register and reset; Keycloak only shows the sign-in form.',
    'No app-side password storage.',
  ],
  cons: [
    'The app now holds reset-code security (expiry, tries, rate limiting, enumeration) like Architecture 4.',
    'Backend service account with manage-users can reset any external password: high-value secret.',
    'Retains all two-domain trade-offs (DNS complexity, disjoint bookmark URLs).',
  ],
  tryThis: [
    'On ext.localhost:8702/architecture-7/ click Forgot Password for carol@partner.example.com, read the code in Mailpit (http://localhost:8125), set a new password, then Sign In.',
    'On the keycloak-2 sign-in page notice there is neither a Register nor a Forgot-password link.',
    'POST /api/architecture-7/self/forgot-password via int.localhost:8701 returns 403.',
  ],
}
