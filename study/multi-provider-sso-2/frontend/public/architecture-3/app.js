/**
 * Frontend application logic for Architecture 3.
 * Used by: Architecture 3 (Two domains: int.localhost:8301 and ext.localhost:8302).
 */

import { render, html } from '../shared/lib.js'
import { getGateway, getMe } from '../shared/api.js'
import { Layout, ExplainPanel, SessionCard, SignedOut } from '../shared/components.js'
import { explain } from './explain.js'

const ARCH = 'architecture-3'

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

    // Both internal and external domains use gateway redirect
    location.href = `/api/${ARCH}/login?next=${encodeURIComponent(location.pathname)}`
    return html`<p aria-busy="true">Redirecting to login...</p>`
  }

  render(
    html`
      <${Layout} title="Architecture 3: Two Domains (Network-Decided)" arch=${ARCH} gw=${gw}>
        <${AppContent} />
      <//>
    `,
    root
  )
}

init().catch(console.error)
