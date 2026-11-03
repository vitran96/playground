# ADR-001: Login Architecture for Internal and External Users

**Status:** Proposed | Accepted
**Date:** YYYY-MM-DD
**Deciders:** Tech Lead, Principal Engineer, Security Architect, Network Lead

## Context
Internal users must be logged in automatically on Active Directory-joined PCs (using SPNEGO/Kerberos where possible); external users and partners must be authenticated with email and password.
The corporate network utilizes Zscaler (ZIA/ZPA) and serves a distributed, global workforce.
This decision evaluates four architectural patterns demonstrated in the `keycloak-login-architectures` reference implementation.

## Decision
We select **Architecture [1 / 2 / 3 / 4]**, because:
- [Justification for the chosen balance of user friction, network setup, and operational complexity]

## Options Considered

| Option | Architecture Pattern | Entry Points | Auto-login UX | Operational Overhead |
|---|---|---|---|---|
| **1** | Single Gateway: Email-First (keycloak-3) | 1 (`localhost:8201`) | Email prompt once, silent redirect to keycloak-1 | Low (single OIDC client, no split DNS) |
| **2** | Single Gateway: User Choice Button (keycloak-4) | 1 (`localhost:8301`) | One click ("Employee"), forwards `kc_idp_hint` | Low (UI choice; risk if user picks wrong) |
| **3** | Two Domains: Network-Decided Routing | 2 (`int.*`, `ext.*`) | True zero-click on internal domain | Medium (requires split DNS or Zscaler ZPA) |
| **4** | Two Domains: App-Owned External Login | 2 (`int.*`, `ext.*`) | True zero-click internal; app owns external | High (app team owns password store & reset) |

## Key Architectural Questions Settled
1. **Zero-Click vs Email-First:** Does entering an email address once per session satisfy the auto-login requirement for internal employees?
2. **Network Routing:** Is the network engineering team available to publish and maintain internal application segments in Zscaler ZPA for the internal entry point?
3. **Identity Broker Firewall:** Is the backchannel firewall path between the broker Keycloaks (`keycloak-2/3/4`) and `keycloak-1` token/JWKS endpoints approved?
4. **Credential Ownership:** Does the product team want to maintain password security (hashing, reset tokens, rate-limiting, MFA) in application code, or delegate completely to Keycloak?

## Consequences
- **Easier:** [What becomes simpler with this choice]
- **Harder:** [What architectural friction or operational duties are introduced]
- **Revisit When:** [Conditions under which this architectural decision should be re-evaluated]

## Action Items
- [ ] Implement production deployment manifests for chosen architecture.
- [ ] Align DNS / Zscaler routing configurations with network team.
- [ ] Finalize AD service account and SPN registration with directory team.
