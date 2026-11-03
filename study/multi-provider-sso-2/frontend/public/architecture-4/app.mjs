// @ts-nocheck
/**
 * Frontend application logic for Architecture 4.
 * Used by: Architecture 4 (Internal: keycloak-1, External: App's own login form).
 */

import { render, html } from '../shared/lib.mjs'
import { getGateway, getMe } from '../shared/api.mjs'
import { Layout, ExplainPanel, SessionCard, SignedOut, LoginForm } from '../shared/components.mjs'
import { explain } from './explain.mjs'

const ARCH = 'architecture-4'

async function init() {
  const root = document.getElementById('root')

  const gwRes = await getGateway(ARCH)
  const gw = gwRes.data

  const meRes = await getMe(ARCH)
  const isLoggedOut = new URLSearchParams(location.search).has('loggedout')

  function AppContent() {
    if (meRes.status === 200 && meRes.data?.user) {
      return html`
        <${SessionCard}
          arch=${ARCH}
          user=${meRes.data.user}
          claims=${meRes.data.claims}
          onLogout=${() => init()}
        />
        <${ExplainPanel} explain=${explain} />
      `
    }

    if (isLoggedOut) {
      return html`
        <${SignedOut} arch=${ARCH} />
        <${ExplainPanel} explain=${explain} />
      `
    }

    if (gw?.loginUi === 'form') {
      return html`
        <${LoginForm} onLoginSuccess=${() => init()} />
        <${ExplainPanel} explain=${explain} />
      `
    }

    // Internal gateway uses OIDC redirect
    location.href = `/api/${ARCH}/login?next=${encodeURIComponent(location.pathname)}`
    return html`<p aria-busy="true">Redirecting to internal corporate login...</p>`
  }

  render(
    html`
      <${Layout} title="Architecture 4: Two Domains with App Login" arch=${ARCH} gw=${gw}>
        <${AppContent} />
      <//>
    `,
    root
  )
}

init().catch(console.error)
