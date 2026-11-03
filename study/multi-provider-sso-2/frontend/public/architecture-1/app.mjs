// @ts-nocheck
/**
 * Frontend application logic for Architecture 1.
 * Used by: Architecture 1 (Email-first via keycloak-3).
 */

import { render, html } from '../shared/lib.mjs'
import { getGateway, getMe } from '../shared/api.mjs'
import { Layout, ExplainPanel, SessionCard, SignedOut } from '../shared/components.mjs'
import { explain } from './explain.mjs'

const ARCH = 'architecture-1'

async function init() {
  const root = document.getElementById('root')

  // 1. Discover which gateway we are behind
  const gwRes = await getGateway(ARCH)
  const gw = gwRes.data

  // 2. Check if active backend session exists
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

    // Auto-redirect to gateway OIDC flow
    location.href = `/api/${ARCH}/login?next=${encodeURIComponent(location.pathname)}`
    return html`<p aria-busy="true">Redirecting to login...</p>`
  }

  render(
    html`
      <${Layout} title="Architecture 1: Email-First Routing" arch=${ARCH} gw=${gw}>
        <${AppContent} />
      <//>
    `,
    root
  )
}

init().catch(console.error)
