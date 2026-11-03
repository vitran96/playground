// @ts-nocheck
/**
 * Frontend application logic for Architecture 6.
 * Used by: Architecture 6 (int.localhost:8601 -> keycloak-1, ext.localhost:8602 -> app login page + keycloak-2).
 */

import { render, html } from '../shared/lib.mjs'
import { getGateway, getMe } from '../shared/api.mjs'
import { Layout, ExplainPanel, SessionCard, SignedOut, AppLoginPage } from '../shared/components.mjs'
import { explain } from './explain.mjs'

const ARCH = 'architecture-6'

async function init() {
  const root = document.getElementById('root')

  const gwRes = await getGateway(ARCH)
  const gw = gwRes.data

  const meRes = await getMe(ARCH)
  const isLoggedOut = new URLSearchParams(location.search).has('loggedout')

  function AppContent() {
    if (meRes.status === 200 && meRes.data?.user) {
      return html`
        <${SessionCard} arch=${ARCH} user=${meRes.data.user} claims=${meRes.data.claims} onLogout=${() => init()} />
        <${ExplainPanel} explain=${explain} />
      `
    }

    if (isLoggedOut) {
      return html`
        <${SignedOut} arch=${ARCH} />
        <${ExplainPanel} explain=${explain} />
      `
    }

    if (gw?.loginUi === 'choice') {
      return html`
        <${AppLoginPage} arch=${ARCH} />
        <${ExplainPanel} explain=${explain} />
      `
    }

    // Internal gateway: straight to keycloak-1, as in Architecture 3
    location.href = `/api/${ARCH}/login?next=${encodeURIComponent(location.pathname)}`
    return html`<p aria-busy="true">Redirecting to internal corporate login...</p>`
  }

  render(
    html`
      <${Layout} title="Architecture 6: Two Domains, App-Owned Registration" arch=${ARCH} gw=${gw}>
        <${AppContent} />
      <//>
    `,
    root
  )
}

init().catch(console.error)
