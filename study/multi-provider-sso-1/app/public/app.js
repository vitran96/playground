import { html, render, useState, useEffect } from 'https://unpkg.com/htm@3/preact/standalone.module.js';

function Nav({ currentPath, navigate }) {
  return html`
    <header class="site-header">
      <nav class="container">
        <ul>
          <li><strong>OIDC Demo</strong></li>
        </ul>
        <ul>
          <li><a href="/" onClick=${(e) => { e.preventDefault(); navigate('/'); }} class=${currentPath === '/' ? 'contrast' : ''}>Home</a></li>
          <li><a href="/architecture-1" onClick=${(e) => { e.preventDefault(); navigate('/architecture-1'); }} class=${currentPath === '/architecture-1' ? 'contrast' : ''}>Architecture 1</a></li>
          <li><a href="/architecture-2" onClick=${(e) => { e.preventDefault(); navigate('/architecture-2'); }} class=${currentPath === '/architecture-2' ? 'contrast' : ''}>Architecture 2</a></li>
        </ul>
      </nav>
    </header>
  `;
}

function ErrorBanner() {
  const params = new URLSearchParams(window.location.search);
  const error = params.get('error');
  if (!error) return null;

  let explanation = `Authentication error: ${error}`;
  if (error === 'no_role') {
    explanation = 'User rejected (fail closed): The ID token did not contain an authorized app role (internal_user or external_resolver).';
  } else if (error === 'state_mismatch') {
    explanation = 'Invalid session state: The returned OAuth state did not match the pending login request.';
  }

  return html`
    <div style="background-color: #ffebee; color: #c62828; border: 1px solid #ef9a9a; border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem;">
      <strong>Sign-in rejected:</strong> ${explanation}
    </div>
  `;
}

function FlowSteps({ arch }) {
  if (arch === 1) {
    return html`
      <details open style="margin-bottom: 1.5rem;">
        <summary><strong>Flow Steps (Architecture 1 · Central Broker)</strong></summary>
        <ol style="margin-top: 0.5rem; padding-left: 1.5rem;">
          <li>App initiates OIDC code flow to <code>kc-central</code> (broker realm).</li>
          <li>Central broker presents IdP options: <strong>Staff</strong> (internal) or <strong>Partner</strong> (external).</li>
          <li>Browser is redirected to the selected IdP (<code>kc-internal</code> or <code>kc-external</code>) to sign in.</li>
          <li>IdP authenticates the user and redirects back to <code>kc-central</code>.</li>
          <li><code>kc-central</code> maps upstream identity to a realm role (<code>internal_user</code> or <code>external_resolver</code>) and redirects back to App.</li>
          <li>App backend verifies ID token from central broker and reads the <code>roles</code> claim to decide user type.</li>
        </ol>
      </details>
    `;
  }
  return html`
    <details open style="margin-bottom: 1.5rem;">
      <summary><strong>Flow Steps (Architecture 2 · Direct Providers)</strong></summary>
      <ol style="margin-top: 0.5rem; padding-left: 1.5rem;">
        <li>User clicks either "Sign in as internal" or "Sign in as external".</li>
        <li>App initiates OIDC code flow directly to <code>kc-internal</code> or <code>kc-external</code>.</li>
        <li>User authenticates directly at that realm.</li>
        <li>Browser returns to provider-specific callback (<code>/auth/a2/internal/callback</code> or <code>/auth/a2/external/callback</code>).</li>
        <li>App backend completes code exchange exclusively with that provider and verifies token <code>iss</code>.</li>
        <li>App backend decides user type from the callback route that completed the flow (confirmed by <code>iss</code>).</li>
      </ol>
    </details>
  `;
}

function IdentityCard({ me }) {
  const isInternal = me.userType === 'internal';
  const badgeColor = isInternal ? '#2e7d32' : '#d84315';
  const badgeBg = isInternal ? '#e8f5e9' : '#fbe9e7';

  return html`
    <article style="margin-bottom: 2rem;">
      <header style="display: flex; justify-content: space-between; align-items: center;">
        <h3 style="margin-bottom: 0;">Identity Card</h3>
        <span style="display: inline-block; padding: 0.35rem 0.75rem; border-radius: 9999px; font-weight: bold; font-size: 0.9rem; text-transform: uppercase; color: ${badgeColor}; background-color: ${badgeBg}; border: 1px solid ${badgeColor};">
          ${me.userType}
        </span>
      </header>
      <table>
        <tbody>
          <tr><th style="width: 220px;">Decided by</th><td><strong>${me.decidedBy}</strong></td></tr>
          <tr><th>Reason</th><td><code>${me.reason}</code></td></tr>
          <tr><th>Login route</th><td><code>${me.loginRoute}</code></td></tr>
          <tr><th>User key (iss|sub)</th><td><code style="word-break: break-all;">${me.user.key}</code></td></tr>
          <tr><th>Full Name</th><td>${me.user.name}</td></tr>
          <tr><th>Email</th><td>${me.user.email}</td></tr>
          <tr><th>Username</th><td>${me.user.username}</td></tr>
        </tbody>
      </table>
    </article>
  `;
}

function ClaimsTable({ claims }) {
  if (!claims) return null;
  const entries = Object.entries(claims);
  const highlighted = ['iss', 'sub', 'roles', 'idp'];

  return html`
    <article style="margin-bottom: 2rem;">
      <header><h3 style="margin-bottom: 0;">Verified ID Token Claims</h3></header>
      <table>
        <thead>
          <tr><th style="width: 220px;">Claim</th><th>Value</th></tr>
        </thead>
        <tbody>
          ${entries.map(([key, val]) => {
            const isHighlight = highlighted.includes(key);
            const valStr = typeof val === 'object' ? JSON.stringify(val) : String(val);
            return html`
              <tr style=${isHighlight ? 'background-color: rgba(2, 136, 209, 0.08); font-weight: 500;' : ''}>
                <td>
                  <code>${key}</code>
                  ${isHighlight ? html`<span style="margin-left: 0.5rem; font-size: 0.7rem; padding: 0.1rem 0.4rem; background: #0288d1; color: white; border-radius: 4px;">key claim</span>` : ''}
                </td>
                <td><code style="word-break: break-all;">${valStr}</code></td>
              </tr>
            `;
          })}
        </tbody>
      </table>
    </article>
  `;
}

function TicketsPanel({ arch, userType }) {
  const [tickets, setTickets] = useState([]);
  const [noteTexts, setNoteTexts] = useState({});
  const [noteStatus, setNoteStatus] = useState({});
  const [loading, setLoading] = useState(true);

  const fetchTickets = () => {
    fetch(`/api/a${arch}/tickets`, { credentials: 'same-origin' })
      .then(res => res.ok ? res.json() : [])
      .then(data => { setTickets(data); setLoading(false); })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchTickets();
  }, [arch]);

  const handleAddNote = async (ticketId) => {
    const text = noteTexts[ticketId];
    if (!text) return;
    try {
      const res = await fetch(`/api/a${arch}/tickets/${ticketId}/internal-note`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ text })
      });
      if (res.status === 201) {
        setNoteStatus(prev => ({ ...prev, [ticketId]: { type: 'success', msg: 'Note added!' } }));
        setNoteTexts(prev => ({ ...prev, [ticketId]: '' }));
        fetchTickets();
      } else if (res.status === 403) {
        setNoteStatus(prev => ({ ...prev, [ticketId]: { type: 'error', msg: '403 Forbidden: External users cannot add internal notes' } }));
      } else {
        const data = await res.json();
        setNoteStatus(prev => ({ ...prev, [ticketId]: { type: 'error', msg: `Error: ${data.error}` } }));
      }
    } catch (err) {
      setNoteStatus(prev => ({ ...prev, [ticketId]: { type: 'error', msg: `Request failed: ${err.message}` } }));
    }
  };

  if (loading) return html`<p>Loading tickets...</p>`;

  return html`
    <article style="margin-bottom: 2rem;">
      <header>
        <h3 style="margin-bottom: 0;">Tickets</h3>
        <small style="color: var(--pico-muted-color);">Role-based visibility: Internal users see all tickets and comments. External users see assigned tickets with shared comments only.</small>
      </header>
      <div style="display: flex; flex-direction: column; gap: 1.5rem;">
        ${tickets.length === 0 ? html`<p>No tickets visible for your account.</p>` : ''}
        ${tickets.map(t => html`
          <div style="border: 1px solid var(--pico-muted-border-color); border-radius: 8px; padding: 1rem;">
            <div style="display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 0.5rem;">
              <h4 style="margin-bottom: 0;">${t.id}: ${t.title}</h4>
              <span style="font-size: 0.85rem; padding: 0.2rem 0.5rem; border-radius: 4px; background: var(--pico-primary-background); color: var(--pico-primary);">${t.status}</span>
            </div>
            <p style="margin-bottom: 0.5rem; font-size: 0.85rem; color: var(--pico-muted-color);">Assignee: <code>${t.assigneeEmail}</code></p>
            <div style="margin-top: 0.75rem;">
              <strong>Comments:</strong>
              <div style="display: flex; flex-direction: column; gap: 0.5rem; margin-top: 0.5rem;">
                ${t.comments.map(c => html`
                  <div style="padding: 0.5rem 0.75rem; border-radius: 6px; background-color: ${c.type === 'internal' ? '#fff3e0' : '#f5f5f5'}; border-left: 4px solid ${c.type === 'internal' ? '#fb8c00' : '#757575'};">
                    <div style="display: flex; justify-content: space-between; font-size: 0.75rem; margin-bottom: 0.25rem;">
                      <span><strong>${c.author}</strong></span>
                      <span style="font-weight: bold; text-transform: uppercase; color: ${c.type === 'internal' ? '#e65100' : '#616161'};">${c.type} comment</span>
                    </div>
                    <div style="font-size: 0.9rem;">${c.text}</div>
                  </div>
                `)}
              </div>
            </div>
            <div style="margin-top: 1rem; padding-top: 0.75rem; border-top: 1px dashed var(--pico-muted-border-color);">
              <form onSubmit=${(e) => { e.preventDefault(); handleAddNote(t.id); }} style="display: flex; gap: 0.5rem; margin-bottom: 0.5rem;">
                <input
                  type="text"
                  placeholder="Internal note text..."
                  value=${noteTexts[t.id] || ''}
                  onInput=${(e) => setNoteTexts({ ...noteTexts, [t.id]: e.target.value })}
                  style="margin-bottom: 0;"
                />
                <button type="submit" style="width: auto; white-space: nowrap;">Add internal note</button>
              </form>
              ${noteStatus[t.id] ? html`
                <div style="font-size: 0.85rem; padding: 0.4rem 0.6rem; border-radius: 4px; ${noteStatus[t.id].type === 'error' ? 'background: #ffebee; color: #c62828;' : 'background: #e8f5e9; color: #2e7d32;'}">
                  ${noteStatus[t.id].msg}
                </div>
              ` : ''}
            </div>
          </div>
        `)}
      </div>
    </article>
  `;
}

function HomeScreen({ navigate }) {
  return html`
    <div class="container" style="padding-top: 2rem;">
      <hgroup>
        <h2>OIDC Multi-Provider Architecture Demo</h2>
        <p>Demonstrating two patterns for single vs multiple identity providers with server-side classification.</p>
      </hgroup>
      <div class="grid" style="margin-top: 2rem;">
        <article>
          <header><h3>Architecture 1: Central Broker</h3></header>
          <p>One OIDC provider (broker). Staff and partners choose their home organisation at the central realm.</p>
          <ul>
            <li>One client ID and redirect URI in the app.</li>
            <li>Central broker issues the ID token with mapped <code>roles</code>.</li>
            <li>Backend decides user type by inspecting the <code>roles</code> claim.</li>
          </ul>
          <footer>
            <a role="button" href="/architecture-1" onClick=${(e) => { e.preventDefault(); navigate('/architecture-1'); }}>Open Architecture 1</a>
          </footer>
        </article>
        <article>
          <header><h3>Architecture 2: Direct Providers</h3></header>
          <p>Two OIDC providers. App communicates directly with internal and external realms.</p>
          <ul>
            <li>Two separate login buttons and callback routes in the app.</li>
            <li>Tokens issued directly by staff or partner Keycloak realm.</li>
            <li>Backend decides user type from callback route + confirmed <code>iss</code>.</li>
          </ul>
          <footer>
            <a role="button" class="secondary" href="/architecture-2" onClick=${(e) => { e.preventDefault(); navigate('/architecture-2'); }}>Open Architecture 2</a>
          </footer>
        </article>
      </div>
    </div>
  `;
}

function Arch1Screen() {
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('/api/a1/me', { credentials: 'same-origin' })
      .then(res => res.ok ? res.json() : null)
      .then(data => { setMe(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, []);

  return html`
    <div class="container" style="padding-top: 1.5rem;">
      <hgroup>
        <h2>Architecture 1: Central Broker (1 Provider)</h2>
        <p>User type is decided by the <code>roles</code> claim in the ID token issued by central broker.</p>
      </hgroup>
      <${ErrorBanner} />
      <${FlowSteps} arch=${1} />
      ${loading ? html`<p>Checking session...</p>` : ''}
      ${!loading && !me ? html`
        <article style="text-align: center; padding: 3rem 1rem;">
          <h3 style="margin-bottom: 1rem;">Not signed in</h3>
          <p style="margin-bottom: 1.5rem;">Authenticate through the central identity broker.</p>
          <a role="button" href="/auth/a1/login" style="padding: 0.75rem 2rem;">Sign in</a>
        </article>
      ` : ''}
      ${!loading && me ? html`
        <div>
          <div style="display: flex; justify-content: flex-end; margin-bottom: 1rem;">
            <a role="button" class="outline contrast" href="/auth/a1/logout">Sign out</a>
          </div>
          <${IdentityCard} me=${me} />
          <${ClaimsTable} claims=${me.idTokenClaims} />
          <${TicketsPanel} arch=${1} userType=${me.userType} />
        </div>
      ` : ''}
    </div>
  `;
}

function Arch2Screen() {
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('/api/a2/me', { credentials: 'same-origin' })
      .then(res => res.ok ? res.json() : null)
      .then(data => { setMe(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, []);

  return html`
    <div class="container" style="padding-top: 1.5rem;">
      <hgroup>
        <h2>Architecture 2: Direct Providers (2 Providers)</h2>
        <p>User type is decided by the callback route that completed the flow, confirmed by <code>iss</code>.</p>
      </hgroup>
      <${ErrorBanner} />
      <${FlowSteps} arch=${2} />
      ${loading ? html`<p>Checking session...</p>` : ''}
      ${!loading && !me ? html`
        <article style="text-align: center; padding: 3rem 1rem;">
          <h3 style="margin-bottom: 1rem;">Not signed in</h3>
          <p style="margin-bottom: 1.5rem;">Choose your sign-in method to authenticate directly with that provider:</p>
          <div style="display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap;">
            <a role="button" href="/auth/a2/internal/login" style="background-color: #2e7d32; border-color: #2e7d32; padding: 0.75rem 1.5rem;">Sign in as internal</a>
            <a role="button" href="/auth/a2/external/login" style="background-color: #d84315; border-color: #d84315; padding: 0.75rem 1.5rem;">Sign in as external</a>
          </div>
        </article>
      ` : ''}
      ${!loading && me ? html`
        <div>
          <div style="display: flex; justify-content: flex-end; margin-bottom: 1rem;">
            <a role="button" class="outline contrast" href="/auth/a2/logout">Sign out</a>
          </div>
          <${IdentityCard} me=${me} />
          <${ClaimsTable} claims=${me.idTokenClaims} />
          <${TicketsPanel} arch=${2} userType=${me.userType} />
        </div>
      ` : ''}
    </div>
  `;
}

function App() {
  const [path, setPath] = useState(window.location.pathname);

  const navigate = (newPath) => {
    window.history.pushState({}, '', newPath);
    setPath(newPath);
  };

  useEffect(() => {
    const onPop = () => setPath(window.location.pathname);
    window.addEventListener('popstate', onPop);
    return () => window.removeEventListener('popstate', onPop);
  }, []);

  let screen;
  if (path === '/architecture-1') {
    screen = html`<${Arch1Screen} />`;
  } else if (path === '/architecture-2') {
    screen = html`<${Arch2Screen} />`;
  } else {
    screen = html`<${HomeScreen} navigate=${navigate} />`;
  }

  return html`
    <div>
      <${Nav} currentPath=${path} navigate=${navigate} />
      <main>
        ${screen}
      </main>
      <footer class="site-footer" style="margin-top: 3rem; padding: 1.5rem 0; text-align: center;">
        <div class="container">
          <small>OIDC Multi-Provider Architecture Demo</small>
        </div>
      </footer>
    </div>
  `;
}

render(html`<${App} />`, document.getElementById('app'));
