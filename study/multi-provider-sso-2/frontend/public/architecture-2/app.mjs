// @ts-nocheck
/**
 * Frontend application logic for Architecture 2.
 * Used by: Architecture 2 (User choice with kc_idp_hint).
 */

import { render, html } from '../shared/lib.mjs'
import { getGateway, getMe } from '../shared/api.mjs'
import { Layout, ExplainPanel, SessionCard, SignedOut, ChooseLogin } from '../shared/components.mjs'
import { explain } from './explain.mjs'

const ARCH = 'architecture-2'

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

    // Architecture 2 presents choice buttons in the frontend
    return html`
      <${ChooseLogin} arch=${ARCH} />
      <${ExplainPanel} explain=${explain} />
    `
  }

  render(
    html`
      <${Layout} title="Architecture 2: User-Selected IdP Choice" arch=${ARCH} gw=${gw}>
        <${AppContent} />
      <//>
    `,
    root
  )
}

init().catch(console.error)
