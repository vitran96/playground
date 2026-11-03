// @ts-nocheck
/**
 * Frontend application logic for Architecture 7.
 * Used by: Architecture 7 (int.localhost:8701 -> keycloak-1, ext.localhost:8702 -> app login page + keycloak-2).
 */

import { render, html } from '../shared/lib.mjs'
import { getGateway, getMe } from '../shared/api.mjs'
import { Layout, ExplainPanel, SessionCard, SignedOut, AppLoginPage } from '../shared/components.mjs'
import { explain } from './explain.mjs'

const ARCH = 'architecture-7'

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
        <${AppLoginPage} arch=${ARCH} allowReset=${true} />
        <${ExplainPanel} explain=${explain} />
      `
    }

    // Internal gateway: straight to keycloak-1, as in Architecture 3
    location.href = `/api/${ARCH}/login?next=${encodeURIComponent(location.pathname)}`
    return html`<p aria-busy="true">Redirecting to internal corporate login...</p>`
  }

  render(
    html`
      <${Layout} title="Architecture 7: Two Domains, App-Owned Registration & Reset" arch=${ARCH} gw=${gw}>
        <${AppContent} />
      <//>
    `,
    root
  )
}

init().catch(console.error)
