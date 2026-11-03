// @ts-nocheck
/**
 * Architecture 6 details and trade-offs.
 * Used by: Architecture 6 frontend and documentation.
 */

export const explain = {
  title: 'Architecture 6: Two Domains, Keycloak Login with App-Owned Registration',
  summary:
    'Architecture 3 with an app-owned front door on the external domain: the app shows its own login page (Sign In / Register). Sign-in still happens in keycloak-2; registration is done by the app, which creates the user in keycloak-2 and emails a temporary password.',
  diagram: `flowchart LR
    subgraph INT["Internal domain: int.localhost:8601"]
      BI["Employee browser"] --> GI["gw-6-int"]
    end
    subgraph EXT["External domain: ext.localhost:8602"]
      BX["Partner browser<br/>app login page"] --> GX["gw-6-ext"]
    end
    GI -- OIDC --> K1["keycloak-1<br/>SPNEGO / password"]
    GX -- "OIDC (Sign In)" --> K2["keycloak-2<br/>email + password<br/>KC forgot-password"]
    GI -- "X-Gateway-Id: gw-6-int" --> BE["Backend"]
    GX -- "POST /self/register" --> BE
    BE -- "admin API: create user<br/>(temporary password)" --> K2
    BE -- "temporary password" --> MP[("Mailpit")]
    BE -. "architecture-6-internal-session<br/>aud architecture-6:internal" .-> BI
    BE -. "architecture-6-external-session<br/>aud architecture-6:external" .-> BX`,
  flow: [
    'Internal Flow (http://int.localhost:8601): unchanged from Architecture 3, gw-6-int goes straight to keycloak-1 (zero-click SPNEGO on domain PCs).',
    'External Flow (http://ext.localhost:8602):',
    '1. The app does not redirect immediately; it shows its own login page with Sign In and Register buttons.',
    '2. Sign In: gw-6-ext protects /api/architecture-6/login and runs OIDC against keycloak-2 (client app-arch6-ext). Forgot password is keycloak-2\'s own email-OTP reset.',
    '3. Register: POST /api/architecture-6/self/register (name + email). The backend creates the user in keycloak-2 via the backend-admin service account with a random temporary password and role partner, then emails the password via Mailpit.',
    '4. At first sign-in keycloak-2 forces the user to choose a new password (temporary credential).',
    '5. Backend issues cookie architecture-6-external-session with audience architecture-6:external.',
  ],
  // Answers to shared CHECKS: auto-login, internal KC exposed, manual login exposed, stolen JWT reusable, KC manages external, registration in app, reset in app (null = N/A)
  checklist: [true, false, false, false, true, true, false],
  status: 'review', // 'none' | 'review' | { skip: '<reason>' }
  notes: [
    'Stolen-JWT replay across zones is avoided exactly as in Architecture 3 (X-Gateway-Id + per-zone JWT audience).',
    'keycloak-1 stays internal-only; keycloak-2 is the only internet-facing IdP and still owns credentials, brute-force detection and password reset.',
    'Registration UX is fully app-controlled (fields, validation, branding) while passwords never touch the app database.',
    'The keycloak-2 Register link is hidden for this client via the arch6-external login theme. The realm itself still allows self-registration (shared with Architecture 3), so in production disable it on a dedicated realm.',
    'The backend needs a keycloak-2 service account with manage-users; that credential becomes a high-value secret.',
  ],
  pros: [
    'Internal users keep true zero-click login.',
    'App owns the onboarding UX without owning credentials or the login itself.',
    'Password reset, lockout and MFA remain Keycloak features.',
  ],
  cons: [
    'Emailing a password (even temporary) is weaker than an email-verification link.',
    'Registration logic and anti-abuse (rate limits, captcha, enumeration) move into the app.',
    'Two places to change for the external UX (app page and Keycloak theme).',
    'Retains all two-domain trade-offs (DNS complexity, disjoint bookmark URLs).',
  ],
  tryThis: [
    'Open ext.localhost:8602/architecture-6/, click Register, read the temporary password in Mailpit (http://localhost:8125), then Sign In and set a new password.',
    'On the keycloak-2 sign-in page notice there is no Register link, but "Forgot password" is still there.',
    'Sign in on int.localhost:8601/architecture-6/ as alice: same zero-click flow as Architecture 3.',
  ],
}
