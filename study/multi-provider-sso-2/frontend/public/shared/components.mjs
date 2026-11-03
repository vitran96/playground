// @ts-nocheck
/**
 * Shared Preact UI components.
 * Used by: Architectures 1, 2, 3, 4.
 *
 * Implements Layout, ExplainPanel, SessionCard, AdminPanel, LoginForm, SignedOut, and ChooseLogin.
 */

import { html, useState } from './lib.mjs'
import { getHello, logout, localLogin, localSignup, localForgotPassword, localResetPassword, adminCreateUser, selfRegister, selfForgotPassword, selfResetPassword } from './api.mjs'

export function Layout({ title, arch, gw, children }) {
  const zone = gw?.zone || 'any'
  const zoneColors = {
    internal: { bg: '#2e7d32', text: '#ffffff', label: 'Internal Zone' },
    external: { bg: '#1565c0', text: '#ffffff', label: 'External Zone' },
    any: { bg: '#616161', text: '#ffffff', label: 'Any Zone' },
  }
  const badge = zoneColors[zone] || zoneColors.any

  return html`
    <header class="container" style="margin-top: 1.5rem; margin-bottom: 1.5rem; border-bottom: 1px solid var(--pico-muted-border-color); padding-bottom: 1rem;">
      <nav style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap;">
        <div>
          <a href="http://localhost:8180" style="font-size: 0.85rem; text-decoration: none; color: var(--pico-secondary);">← All Architectures Portal</a>
          <h2 style="margin: 0.25rem 0 0 0;">${title}</h2>
        </div>
        <div style="display: flex; gap: 0.75rem; align-items: center; margin-top: 0.5rem;">
          <span style="font-size: 0.85rem; font-family: monospace; background: var(--pico-card-background-color); padding: 0.25rem 0.5rem; border-radius: 4px; border: 1px solid var(--pico-muted-border-color);">
            GW: <strong>${gw?.id || 'loading...'}</strong>
          </span>
          <span style="font-size: 0.85rem; font-weight: bold; background: ${badge.bg}; color: ${badge.text}; padding: 0.25rem 0.6rem; border-radius: 4px;">
            ${badge.label}
          </span>
        </div>
      </nav>
    </header>
    <main class="container">
      ${children}
    </main>
    <footer class="container" style="margin-top: 3rem; margin-bottom: 2rem; text-align: center; font-size: 0.8rem; color: var(--pico-muted-color);">
      Keycloak Login Architectures Demo · Hono + Apache mod_auth_openidc + Preact
    </footer>
  `
}

// want: desired answer; null = either answer is fine
export const CHECKS = [
  { q: 'Can internal users auto-login (no email typing / IdP choice)?', want: true },
  { q: 'Is the internal Keycloak exposed to the internet?', want: false },
  { q: "Is internal users' manual (password) login exposed to the internet?", want: false },
  { q: "Can an external user reuse a stolen internal user's app JWT to log in as them?", want: false },
  { q: 'Does Keycloak manage external users?', want: null },
  { q: 'Is registration in the app?', want: null },
  { q: 'Is password reset in the app?', want: null },
]

export function checkScore(answers) {
  const ok = (i) => CHECKS[i].want === null || answers[i] === CHECKS[i].want
  const passed = CHECKS.filter((_, i) => ok(i)).length
  const level = ok(0) && ok(1) && ok(2) ? 'green'
    : !ok(2) || [0, 1, 2, 3].every((i) => !ok(i)) ? 'red'
    : 'yellow'
  return { passed, total: CHECKS.length, level }
}

const CHIP = {
  green: { bg: '#2e7d32', fg: '#fff', icon: '✔' },
  yellow: { bg: '#f9a825', fg: '#000', icon: '⚠' },
  red: { bg: '#c62828', fg: '#fff', icon: '✖' },
}

export function ScoreChip({ answers }) {
  if (!answers) return null
  const { passed, total, level } = checkScore(answers)
  const c = CHIP[level]
  return html`<span style="display: inline-block; margin-left: 0.5rem; padding: 0.1rem 0.5rem; border-radius: 999px; font-size: 0.8rem; font-weight: bold; background: ${c.bg}; color: ${c.fg};">${c.icon} ${passed}/${total}</span>`
}

// status: 'none' | 'review' | { skip: '<reason>' }
export function StatusChip({ status }) {
  const label = status === 'review' ? 'Need review' : status?.skip ? `Skip: ${status.skip}` : null
  if (!label) return null
  const bg = status === 'review' ? '#6a1b9a' : '#616161'
  return html`<span style="display: inline-block; margin-left: 0.5rem; padding: 0.1rem 0.5rem; border-radius: 999px; font-size: 0.8rem; font-weight: bold; background: ${bg}; color: #fff;">${label}</span>`
}

// v null = not relevant (N/A)
const yesNo = (v, want) => v === null
  ? html`<span style="color: var(--pico-muted-color);">N/A</span>`
  : html`<span style="color: ${want === null || v === want ? '#2e7d32' : '#c62828'};">${v ? 'Yes' : 'No'}</span>`

export function ChecklistSummary({ archs }) {
  return html`<table style="font-size: 0.9rem;">
    <thead><tr><th>Question</th><th>Should be</th>${archs.map((a) => html`<th>${a.name.replace('architecture-', 'Arch ')}</th>`)}</tr></thead>
    <tbody>
      ${CHECKS.map((c, i) => html`<tr>
        <td>${c.q}</td>
        <td>${c.want === null ? 'Either' : c.want ? 'Yes' : 'No'}</td>
        ${archs.map((a) => html`<td>${yesNo(a.checklist[i], c.want)}</td>`)}
      </tr>`)}
      <tr><td><strong>Score</strong></td><td></td>${archs.map((a) => html`<td><${ScoreChip} answers=${a.checklist} /></td>`)}</tr>
    </tbody>
  </table>`
}

export function ExplainPanel({ explain }) {
  if (!explain) return null
  return html`
    <article style="margin-top: 2rem; background: var(--pico-card-background-color); border: 1px solid var(--pico-muted-border-color);">
      <h3 style="margin-bottom: 1rem;">Architecture Explanation</h3>
      <p><strong>Summary:</strong> ${explain.summary}</p>

      ${explain.checklist && html`
        <details open>
          <summary><strong>Checklist</strong><${ScoreChip} answers=${explain.checklist} /></summary>
          <table style="margin-top: 0.5rem; font-size: 0.9rem;">
            <thead><tr><th>Question</th><th>Answer</th><th>Should be</th></tr></thead>
            <tbody>
              ${CHECKS.map((c, i) => html`<tr>
                <td>${c.q}</td>
                <td>${yesNo(explain.checklist[i], c.want)}</td>
                <td>${c.want === null ? 'Either' : c.want ? 'Yes' : 'No'}</td>
              </tr>`)}
            </tbody>
          </table>
        </details>
      `}

      <details open>
        <summary><strong>Authentication Flow</strong></summary>
        <ol style="margin-top: 0.5rem; padding-left: 1.25rem;">
          ${(explain.flow || []).map((step) => html`<li style="margin-bottom: 0.35rem;">${step}</li>`)}
        </ol>
      </details>

      <details open style="margin-top: 1rem;">
        <summary><strong>Trade-offs & Review Notes</strong></summary>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 0.5rem;">
          <div style="background: rgba(46, 125, 50, 0.08); padding: 0.75rem; border-radius: 4px;">
            <strong style="color: #2e7d32;">Pros</strong>
            <ul style="margin-top: 0.25rem; padding-left: 1rem;">
              ${(explain.pros || []).map((p) => html`<li>${p}</li>`)}
            </ul>
          </div>
          <div style="background: rgba(198, 40, 40, 0.08); padding: 0.75rem; border-radius: 4px;">
            <strong style="color: #c62828;">Cons</strong>
            <ul style="margin-top: 0.25rem; padding-left: 1rem;">
              ${(explain.cons || []).map((c) => html`<li>${c}</li>`)}
            </ul>
          </div>
        </div>
        <div style="margin-top: 0.75rem; padding: 0.75rem; background: rgba(2, 136, 209, 0.08); border-radius: 4px;">
          <strong style="color: #0288d1;">Implementation Notes</strong>
          <ul style="margin-top: 0.25rem; padding-left: 1rem; margin-bottom: 0;">
            ${(explain.notes || []).map((n) => html`<li>${n}</li>`)}
          </ul>
        </div>
      </details>

      <details open style="margin-top: 1rem;">
        <summary><strong>Try This (Acceptance Checks)</strong></summary>
        <ul style="margin-top: 0.5rem; padding-left: 1.25rem; margin-bottom: 0;">
          ${(explain.tryThis || []).map((t) => html`<li style="margin-bottom: 0.25rem;">${t}</li>`)}
        </ul>
      </details>
    </article>
  `
}

export function SessionCard({ arch, user, claims, onLogout }) {
  const [apiResult, setApiResult] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleCallApi = async () => {
    setLoading(true)
    const res = await getHello(arch)
    setApiResult(res.data)
    setLoading(false)
  }

  const handleLogout = async () => {
    const res = await logout(arch)
    if (res.data?.gatewayLogout) {
      // mod_auth_openidc logout endpoint
      location.href = `/api/${arch}/login/callback?logout=${encodeURIComponent(location.origin + '/' + arch + '/?loggedout=1')}`
    } else {
      location.href = `/${arch}/?loggedout=1`
    }
  }

  const expDate = claims?.exp ? new Date(claims.exp * 1000).toLocaleTimeString() : 'N/A'

  return html`
    <div class="grid">
      <div>
        <article>
          <header style="display: flex; justify-content: space-between; align-items: center;">
            <h3 style="margin: 0;">Active Application Session</h3>
            <span style="font-size: 0.85rem; font-weight: bold; text-transform: uppercase; background: ${user.userType === 'internal' ? '#2e7d32' : '#1565c0'}; color: white; padding: 0.2rem 0.5rem; border-radius: 3px;">
              ${user.userType}
            </span>
          </header>

          <p><strong>Username:</strong> ${user.username}</p>
          <p><strong>Display Name:</strong> ${user.displayName}</p>
          <p><strong>Assigned Roles:</strong> ${(user.roles || []).join(', ') || 'none'}</p>

          <div style="margin-top: 1.5rem; display: flex; gap: 0.75rem;">
            <button class="outline" onClick=${handleCallApi} disabled=${loading}>
              ${loading ? 'Calling API...' : 'Call Protected API (/hello)'}
            </button>
            <button class="secondary" onClick=${handleLogout}>
              Sign Out
            </button>
          </div>

          ${apiResult && html`
            <div style="margin-top: 1rem; padding: 0.75rem; background: var(--pico-card-background-color); border: 1px solid var(--pico-muted-border-color); border-radius: 4px; font-size: 0.9rem;">
              <strong>API Response:</strong>
              <pre style="margin-top: 0.5rem; margin-bottom: 0;">${JSON.stringify(apiResult, null, 2)}</pre>
            </div>
          `}
        </article>
      </div>

      <div>
        <article>
          <header><h4 style="margin: 0;">Decoded App JWT Claims</h4></header>
          <p style="font-size: 0.85rem; color: var(--pico-secondary);">
            Minted by backend, stored in an HttpOnly cookie. Keycloak is not contacted for API calls.
          </p>
          <table style="font-size: 0.85rem;">
            <tbody>
              <tr><td><code>iss</code> (Issuer)</td><td>${claims?.iss}</td></tr>
              <tr><td><code>aud</code> (Audience)</td><td><strong>${claims?.aud}</strong></td></tr>
              <tr><td><code>gw</code> (Gateway ID)</td><td>${claims?.gw}</td></tr>
              <tr><td><code>sub</code> (Subject)</td><td>${claims?.sub}</td></tr>
              <tr><td><code>exp</code> (Expires At)</td><td>${expDate}</td></tr>
            </tbody>
          </table>
        </article>
      </div>
    </div>
    ${(user.roles || []).includes('app-admin') && html`<${AdminPanel} arch=${arch} />`}
  `
}

export function AdminPanel({ arch }) {
  const [userType, setUserType] = useState('external')
  const [email, setEmail] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [result, setResult] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [internalOpen, setInternalOpen] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setResult(null)
    if (userType === 'internal') {
      setInternalOpen(true)
      return
    }
    setSubmitting(true)
    const res = await adminCreateUser(arch, { email, firstName, lastName })
    setSubmitting(false)
    if (res.status === 201) {
      setResult({ ok: true, text: res.data.message })
      setEmail('')
      setFirstName('')
      setLastName('')
    } else {
      setResult({ ok: false, text: res.data?.error || `Request failed (${res.status})` })
    }
  }

  return html`
    <article>
      <header><h3 style="margin: 0;">Admin Dashboard: Register User</h3></header>
      <form onSubmit=${handleSubmit}>
        <fieldset>
          <legend>User type</legend>
          <label><input type="radio" name="userType" checked=${userType === 'external'} onChange=${() => setUserType('external')} /> External (partner / customer)</label>
          <label><input type="radio" name="userType" checked=${userType === 'internal'} onChange=${() => setUserType('internal')} /> Internal (employee)</label>
        </fieldset>
        ${userType === 'external' && html`
          <div class="grid">
            <label>First name<input type="text" required maxlength="100" value=${firstName} onInput=${(e) => setFirstName(e.target.value)} /></label>
            <label>Last name<input type="text" required maxlength="100" value=${lastName} onInput=${(e) => setLastName(e.target.value)} /></label>
          </div>
          <label>Email<input type="email" required value=${email} onInput=${(e) => setEmail(e.target.value)} placeholder="jane@partner.example.com" /></label>
          <small style="display: block; margin-bottom: 1rem; color: var(--pico-muted-color);">Created with default password <code>password</code> and role <code>partner</code>.</small>
        `}
        <button type="submit" disabled=${submitting}>${submitting ? 'Registering...' : userType === 'external' ? 'Register External User' : 'Register Internal User'}</button>
      </form>
      ${result && html`<p style="color: ${result.ok ? '#2e7d32' : '#c62828'}; background: ${result.ok ? 'rgba(46,125,50,0.1)' : 'rgba(198,40,40,0.1)'}; padding: 0.5rem; border-radius: 4px; font-size: 0.9rem;">${result.text}</p>`}

      <dialog open=${internalOpen}>
        <article>
          <header>
            <button aria-label="Close" rel="prev" onClick=${() => setInternalOpen(false)}></button>
            <h4 style="margin: 0;">Internal users are not registered here</h4>
          </header>
          <p>Employees live in the company directory (Active Directory behind keycloak-1), which this app does not own.</p>
          <ul>
            <li>In a real implementation, this button would call an <strong>internal provisioning service</strong> to add the employee.</li>
            <li>Or the employee simply <strong>signs in once</strong>: the app auto-creates their profile with <strong>no app role</strong> until an app admin assigns one.</li>
          </ul>
          <footer><button onClick=${() => setInternalOpen(false)}>OK</button></footer>
        </article>
      </dialog>
    </article>
  `
}

export function SignedOut({ arch }) {
  return html`
    <article style="text-align: center; max-width: 600px; margin: 2rem auto; padding: 2.5rem 1.5rem;">
      <h3 style="color: var(--pico-primary);">You Have Successfully Signed Out</h3>
      <p style="color: var(--pico-secondary); margin-bottom: 1.5rem;">
        Your application session cookie, Apache gateway state, and Keycloak SSO session have been cleared.
      </p>
      <a href="/${arch}/" role="button">Sign In Again</a>
    </article>
  `
}

export function ChooseLogin({ arch }) {
  return html`
    <article style="max-width: 540px; margin: 2rem auto; text-align: center; padding: 2.5rem 2rem;">
      <h3>Choose Your Sign-In Method</h3>
      <p style="color: var(--pico-secondary); margin-bottom: 2rem;">
        Architecture 2 lets users choose their identity provider. The gateway forwards this selection using <code>kc_idp_hint</code>.
      </p>
      <div style="display: flex; flex-direction: column; gap: 1rem;">
        <a href="/api/${arch}/login/internal" role="button" style="background-color: #2e7d32; border-color: #2e7d32; font-weight: bold;">
          Sign in as Employee (Company Account)
        </a>
        <a href="/api/${arch}/login/external" role="button" class="secondary" style="font-weight: bold;">
          Sign in as Partner / External User
        </a>
      </div>
    </article>
  `
}

export function LoginForm({ onLoginSuccess }) {
  const [tab, setTab] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [code, setCode] = useState('')
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const handleLogin = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    const res = await localLogin(email, password)
    setSubmitting(false)
    if (res.status === 200) {
      onLoginSuccess()
    } else {
      setError(res.data?.error || 'Login failed')
    }
  }

  const handleSignup = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    const res = await localSignup(email, password, displayName)
    setSubmitting(false)
    if (res.status === 200) {
      onLoginSuccess()
    } else {
      setError(res.data?.error || 'Signup failed')
    }
  }

  const handleForgot = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    const res = await localForgotPassword(email)
    setSubmitting(false)
    if (res.status === 200) {
      setMessage(res.data?.message || 'If the account exists, a code has been emailed to it.')
      setCode('')
      setTab('reset')
    } else {
      setError(res.data?.error || 'Request failed')
    }
  }

  const handleReset = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    const res = await localResetPassword(email, code, newPassword)
    setSubmitting(false)
    if (res.status === 200) {
      setMessage(res.data?.message || 'Password reset successfully. You can now log in.')
      setTab('login')
    } else {
      setError(res.data?.error || 'Reset failed')
    }
  }

  return html`
    <article style="max-width: 500px; margin: 2rem auto;">
      <header>
        <div style="display: flex; gap: 0.5rem; justify-content: space-around; border-bottom: 1px solid var(--pico-muted-border-color); padding-bottom: 0.75rem;">
          <button class="${tab === 'login' ? '' : 'outline'} secondary" style="font-size: 0.85rem; padding: 0.35rem 0.75rem;" onClick=${() => { setTab('login'); setError(null); setMessage(null); }}>Sign In</button>
          <button class="${tab === 'signup' ? '' : 'outline'} secondary" style="font-size: 0.85rem; padding: 0.35rem 0.75rem;" onClick=${() => { setTab('signup'); setError(null); setMessage(null); }}>Sign Up</button>
          <button class="${tab === 'forgot' ? '' : 'outline'} secondary" style="font-size: 0.85rem; padding: 0.35rem 0.75rem;" onClick=${() => { setTab('forgot'); setError(null); setMessage(null); }}>Forgot Password</button>
        </div>
      </header>

      ${error && html`<p style="color: #c62828; background: rgba(198,40,40,0.1); padding: 0.5rem; border-radius: 4px; font-size: 0.9rem;">${error}</p>`}
      ${message && html`<p style="color: #2e7d32; background: rgba(46,125,50,0.1); padding: 0.5rem; border-radius: 4px; font-size: 0.9rem;">${message}</p>`}

      ${tab === 'login' && html`
        <form onSubmit=${handleLogin}>
          <label>Email Address
            <input type="email" required value=${email} onInput=${(e) => setEmail(e.target.value)} placeholder="erin@partner.example.com" />
          </label>
          <label>Password
            <input type="password" required value=${password} onInput=${(e) => setPassword(e.target.value)} placeholder="••••••••" />
          </label>
          <button type="submit" disabled=${submitting}>${submitting ? 'Authenticating...' : 'Sign In'}</button>
          <small style="display: block; margin-top: 0.5rem; color: var(--pico-muted-color);">Test account: <code>erin@partner.example.com</code> / <code>erin</code></small>
        </form>
      `}

      ${tab === 'signup' && html`
        <form onSubmit=${handleSignup}>
          <label>Display Name
            <input type="text" value=${displayName} onInput=${(e) => setDisplayName(e.target.value)} placeholder="Jane Partner" />
          </label>
          <label>Email Address (Partner / External)
            <input type="email" required value=${email} onInput=${(e) => setEmail(e.target.value)} placeholder="jane@external.example.com" />
          </label>
          <label>Password (min 8 chars)
            <input type="password" required minlength="8" value=${password} onInput=${(e) => setPassword(e.target.value)} />
          </label>
          <button type="submit" disabled=${submitting}>${submitting ? 'Creating account...' : 'Create Account'}</button>
          <small style="display: block; margin-top: 0.5rem; color: var(--pico-muted-color);">Note: <code>@internal.email.com</code> emails are blocked here.</small>
        </form>
      `}

      ${tab === 'forgot' && html`
        <form onSubmit=${handleForgot}>
          <label>Email Address
            <input type="email" required value=${email} onInput=${(e) => setEmail(e.target.value)} placeholder="erin@partner.example.com" />
          </label>
          <button type="submit" disabled=${submitting}>${submitting ? 'Sending code...' : 'Email Me a Reset Code'}</button>
          <small style="display: block; margin-top: 0.5rem; color: var(--pico-muted-color);">A 6-digit code is emailed; open Mailpit at <a href="http://localhost:8125" target="_blank" rel="noopener">localhost:8125</a>.</small>
        </form>
      `}

      ${tab === 'reset' && html`
        <form onSubmit=${handleReset}>
          <label>Email Address
            <input type="email" required value=${email} onInput=${(e) => setEmail(e.target.value)} />
          </label>
          <label>6-digit Code
            <input type="text" required inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" maxlength="6" value=${code} onInput=${(e) => setCode(e.target.value)} />
          </label>
          <label>New Password (min 8 chars)
            <input type="password" required minlength="8" value=${newPassword} onInput=${(e) => setNewPassword(e.target.value)} />
          </label>
          <button type="submit" disabled=${submitting}>${submitting ? 'Resetting...' : 'Update Password'}</button>
          <small style="display: block; margin-top: 0.5rem; color: var(--pico-muted-color);">No code? <a href="#" onClick=${(e) => { e.preventDefault(); setTab('forgot'); setMessage(null); setError(null); }}>Request a new one</a> (max one per minute).</small>
        </form>
      `}
    </article>
  `
}

// Architectures 6 and 7 (external): sign-in redirects to keycloak-2; registration (and reset, Arch 7) stay in the app
export function AppLoginPage({ arch, allowReset }) {
  const [view, setView] = useState(null)
  const [email, setEmail] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const show = (v) => { setView(v); setError(null); setMessage(null) }
  const submit = (call, next) => async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    setMessage(null)
    const res = await call()
    setSubmitting(false)
    if (res.status >= 200 && res.status < 300) {
      setMessage(res.data?.message)
      setCode('')
      setView(next)
    } else {
      setError(res.data?.error || `Request failed (${res.status})`)
    }
  }

  return html`
    <article style="max-width: 500px; margin: 2rem auto;">
      <header><h3 style="margin: 0;">Partner Sign-In</h3></header>
      <p style="color: var(--pico-secondary);">Sign in with keycloak-2. Registration${allowReset ? ' and password reset are' : ' is'} handled here in the app.</p>
      <div style="display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 1rem;">
        <a role="button" href="/api/${arch}/login?next=${encodeURIComponent(`/${arch}/`)}">Sign In</a>
        <button class="secondary ${view === 'register' ? '' : 'outline'}" onClick=${() => show('register')}>Register</button>
        ${allowReset && html`<button class="secondary ${view === 'forgot' || view === 'reset' ? '' : 'outline'}" onClick=${() => show('forgot')}>Forgot Password</button>`}
      </div>

      ${error && html`<p style="color: #c62828; background: rgba(198,40,40,0.1); padding: 0.5rem; border-radius: 4px; font-size: 0.9rem;">${error}</p>`}
      ${message && html`<p style="color: #2e7d32; background: rgba(46,125,50,0.1); padding: 0.5rem; border-radius: 4px; font-size: 0.9rem;">${message}</p>`}

      ${view === 'register' && html`
        <form onSubmit=${submit(() => selfRegister(arch, { email, firstName, lastName }), null)}>
          <div class="grid">
            <label>First name<input type="text" required maxlength="100" value=${firstName} onInput=${(e) => setFirstName(e.target.value)} /></label>
            <label>Last name<input type="text" required maxlength="100" value=${lastName} onInput=${(e) => setLastName(e.target.value)} /></label>
          </div>
          <label>Email Address (Partner / External)
            <input type="email" required value=${email} onInput=${(e) => setEmail(e.target.value)} placeholder="jane@partner.example.com" />
          </label>
          <button type="submit" disabled=${submitting}>${submitting ? 'Creating account...' : 'Create Account'}</button>
          <small style="display: block; margin-top: 0.5rem; color: var(--pico-muted-color);">A temporary password is emailed (see <a href="http://localhost:8125" target="_blank" rel="noopener">Mailpit</a>); keycloak-2 asks for a new one at first sign-in.</small>
        </form>
      `}

      ${view === 'forgot' && html`
        <form onSubmit=${submit(() => selfForgotPassword(arch, email), 'reset')}>
          <label>Email Address
            <input type="email" required value=${email} onInput=${(e) => setEmail(e.target.value)} placeholder="carol@partner.example.com" />
          </label>
          <button type="submit" disabled=${submitting}>${submitting ? 'Sending code...' : 'Email Me a Reset Code'}</button>
        </form>
      `}

      ${view === 'reset' && html`
        <form onSubmit=${submit(() => selfResetPassword(arch, email, code, newPassword), null)}>
          <label>Email Address
            <input type="email" required value=${email} onInput=${(e) => setEmail(e.target.value)} />
          </label>
          <label>6-digit Code
            <input type="text" required inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" maxlength="6" value=${code} onInput=${(e) => setCode(e.target.value)} />
          </label>
          <label>New Password (min 8 chars)
            <input type="password" required minlength="8" value=${newPassword} onInput=${(e) => setNewPassword(e.target.value)} />
          </label>
          <button type="submit" disabled=${submitting}>${submitting ? 'Resetting...' : 'Update Password'}</button>
        </form>
      `}
    </article>
  `
}
