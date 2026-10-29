import { html, render, useState, useEffect } from 'https://unpkg.com/htm@3/preact/standalone.module.js';

function Nav({ currentPath, navigate }) {
  const links = [
    { path: '/', label: 'Home' },
    { path: '/architecture-1', label: 'Arch 1' },
    { path: '/architecture-2', label: 'Arch 2' },
    { path: '/architecture-3', label: 'Arch 3 (2 Traefiks)' },
    { path: '/architecture-4', label: 'Arch 4 (1 Traefik)' },
    { path: '/architecture-5', label: 'Arch 5 (Sub-route)' },
    { path: '/architecture-6', label: 'Arch 6 (Header)' },
    { path: '/architecture-7', label: 'Arch 7 (IdP Hint)' },
  ];

  return html`
    <header class="site-header">
      <nav class="container">
        <ul>
          <li><strong>OIDC Demo</strong></li>
        </ul>
        <ul style="flex-wrap: wrap;">
          ${links.map(l => html`
            <li>
              <a href=${l.path}
                 onClick=${(e) => { e.preventDefault(); navigate(l.path); }}
                 class=${(currentPath === l.path || (l.path !== '/' && currentPath.startsWith(l.path))) ? 'contrast' : ''}>
                ${l.label}
              </a>
            </li>
          `)}
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
  const steps = {
    1: [
      html`App initiates OIDC code flow to <code>kc-central</code> (broker realm).`,
      html`Central broker presents IdP options: <strong>Staff</strong> (internal) or <strong>Partner</strong> (external).`,
      html`Browser is redirected to the selected IdP (<code>kc-internal</code> or <code>kc-external</code>) to sign in.`,
      html`IdP authenticates the user and redirects back to <code>kc-central</code>.`,
      html`<code>kc-central</code> maps upstream identity to a realm role (<code>internal_user</code> or <code>external_resolver</code>) and redirects back to App.`,
      html`App backend verifies ID token from central broker and reads the <code>roles</code> claim to decide user type.`
    ],
    2: [
      html`User clicks either "Sign in as internal" or "Sign in as external".`,
      html`App initiates OIDC code flow directly to <code>kc-internal</code> or <code>kc-external</code>.`,
      html`User authenticates directly at that realm.`,
      html`Browser returns to provider-specific callback (<code>/auth/a2/internal/callback</code> or <code>/auth/a2/external/callback</code>).`,
      html`App backend completes code exchange exclusively with that provider and verifies token <code>iss</code>.`,
      html`App backend decides user type from the callback route that completed the flow (confirmed by <code>iss</code>).`
    ],
    3: [
      html`Traffic is network-isolated across two reverse proxies: <code>traefik-internal</code> (:8083) and <code>traefik-external</code> (:8084).`,
      html`Visiting via internal Traefik automatically routes staff to Corp Intranet (<code>kc-internal</code>).`,
      html`Visiting via external Traefik presents the partner email + password login (<code>kc-external</code>).`,
      html`Both proxies forward to the single unified backend, enforcing boundary at the network gateway.`
    ],
    4: [
      html`Traffic enters a single Traefik instance configured with 2 entrypoint ports: internal (:8085) vs external (:8086).`,
      html`Frontend inspects <code>window.location.port</code> to decide authentication behavior.`,
      html`If port 8085, frontend automatically redirects to internal Keycloak login.`,
      html`If port 8086, frontend renders external email + password login.`
    ],
    5: [
      html`All internal staff interactions are scoped under sub-route <code>/architecture-5/internal</code>.`,
      html`External partner interactions are mounted under parent root <code>/architecture-5</code>.`,
      html`Backend maintains separate session scopes (<code>a5_internal</code> vs <code>a5_external</code>) per sub-route boundary.`
    ],
    6: [
      html`Traefik gateways inject header <code>X-Network-Zone: internal</code> or <code>external</code> into proxied requests.`,
      html`User hits unified backend login endpoint: <code>/auth/a6/login</code>.`,
      html`Backend inspects the <code>X-Network-Zone</code> header and handles redirection automatically without client logic.`
    ],
    7: [
      html`App initiates OIDC code flow to <code>kc-central</code> with query parameter <code>kc_idp_hint=internal</code> or <code>external</code>.`,
      html`Central broker bypasses manual IdP selection and immediately redirects to the target Keycloak realm.`,
      html`Returns through central broker, issuing a token with verified roles while eliminating selection friction.`
    ]
  };

  const currentSteps = steps[arch] || [];
  return html`
    <details open style="margin-bottom: 1.5rem;">
      <summary><strong>Flow Steps (Architecture ${arch})</strong></summary>
      <ol style="margin-top: 0.5rem; padding-left: 1.5rem;">
        ${currentSteps.map(step => html`<li>${step}</li>`)}
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

function TicketsPanel({ arch, userType, apiPrefix }) {
  const [tickets, setTickets] = useState([]);
  const [noteTexts, setNoteTexts] = useState({});
  const [noteStatus, setNoteStatus] = useState({});
  const [loading, setLoading] = useState(true);

  const prefix = apiPrefix || `a${arch}`;
  const fetchTickets = () => {
    fetch(`/api/${prefix}/tickets`, { credentials: 'same-origin' })
      .then(res => res.ok ? res.json() : [])
      .then(data => { setTickets(data); setLoading(false); })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchTickets();
  }, [arch, prefix]);

  const handleAddNote = async (ticketId) => {
    const text = noteTexts[ticketId];
    if (!text) return;
    try {
      const res = await fetch(`/api/${prefix}/tickets/${ticketId}/internal-note`, {
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
  const cards = [
    {
      num: 1,
      title: 'Architecture 1: Central Broker',
      desc: '1 OIDC provider (broker). Staff and partners choose their home organisation at the central realm.',
      points: [
        'Single client ID & redirect URI.',
        'Central broker issues token with mapped roles.',
        'Backend classifies by inspecting roles claim.'
      ],
      path: '/architecture-1'
    },
    {
      num: 2,
      title: 'Architecture 2: Direct Providers',
      desc: '2 OIDC providers. App communicates directly with internal and external realms.',
      points: [
        'Two separate login buttons and callback routes.',
        'Tokens issued directly by staff or partner realm.',
        'Backend classifies from callback route + issuer.'
      ],
      path: '/architecture-2'
    },
    {
      num: 3,
      title: 'Architecture 3: 2 Traefiks',
      desc: 'Two reverse proxies: internal Traefik (:8083) routes to staff IdP; external Traefik (:8084) routes to partner login.',
      points: [
        'Network-level isolation at ingress gateways.',
        'Internal Traefik auto-directs to kc-internal.',
        'External Traefik presents email+password login.'
      ],
      path: '/architecture-3'
    },
    {
      num: 4,
      title: 'Architecture 4: 1 Traefik Domain',
      desc: '1 Traefik with 2 ports: Frontend detects port (:8085 vs :8086) to trigger internal or external login.',
      points: [
        'Single gateway infrastructure.',
        'Frontend checks window.location.port.',
        'Zero backend domain logic needed.'
      ],
      path: '/architecture-4'
    },
    {
      num: 5,
      title: 'Architecture 5: Sub-route',
      desc: 'Sub-route /internal handles staff interactions; parent root /architecture-5 handles external users.',
      points: [
        'Path-based tenant separation on same host.',
        'Internal user scope strictly in /internal.',
        'Isolated session keys and API routes.'
      ],
      path: '/architecture-5'
    },
    {
      num: 6,
      title: 'Architecture 6: Traefik Header',
      desc: 'Traefik injects X-Network-Zone: internal or external. Backend automatically routes /auth/a6/login.',
      points: [
        'Gateway injects network zone header.',
        'Unified client login URL (/auth/a6/login).',
        'Backend enforces routing policy centrally.'
      ],
      path: '/architecture-6'
    },
    {
      num: 7,
      title: 'Architecture 7: Central IdP Hint',
      desc: 'Central broker with kc_idp_hint: central roles mapping with zero manual selection friction.',
      points: [
        'Maintains central token and role federation.',
        'Bypasses Keycloak IdP picker via kc_idp_hint.',
        'Direct login experience with broker governance.'
      ],
      path: '/architecture-7'
    }
  ];

  return html`
    <div class="container" style="padding-top: 2rem;">
      <hgroup>
        <h2>OIDC Multi-Provider Architecture Demo</h2>
        <p>Comparing 7 patterns for internal (staff) vs external (partner) identity provider integration.</p>
      </hgroup>
      <div class="grid" style="margin-top: 2rem;">
        ${cards.slice(0, 2).map(c => html`
          <article>
            <header><h3>${c.title}</h3></header>
            <p>${c.desc}</p>
            <ul>${c.points.map(p => html`<li>${p}</li>`)}</ul>
            <footer>
              <a role="button" href=${c.path} onClick=${(e) => { e.preventDefault(); navigate(c.path); }}>Open Arch ${c.num}</a>
            </footer>
          </article>
        `)}
      </div>
      <div class="grid" style="margin-top: 1rem;">
        ${cards.slice(2, 4).map(c => html`
          <article>
            <header><h3>${c.title}</h3></header>
            <p>${c.desc}</p>
            <ul>${c.points.map(p => html`<li>${p}</li>`)}</ul>
            <footer>
              <a role="button" href=${c.path} onClick=${(e) => { e.preventDefault(); navigate(c.path); }}>Open Arch ${c.num}</a>
            </footer>
          </article>
        `)}
      </div>
      <div class="grid" style="margin-top: 1rem;">
        ${cards.slice(4).map(c => html`
          <article>
            <header><h3>${c.title}</h3></header>
            <p>${c.desc}</p>
            <ul>${c.points.map(p => html`<li>${p}</li>`)}</ul>
            <footer>
              <a role="button" href=${c.path} onClick=${(e) => { e.preventDefault(); navigate(c.path); }}>Open Arch ${c.num}</a>
            </footer>
          </article>
        `)}
      </div>
    </div>
  `;
}

function GenericArchScreen({ arch, title, description, notLoggedInContent, apiPrefix, logoutPath }) {
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);
  const prefix = apiPrefix || `a${arch}`;
  const logout = logoutPath || `/auth/${prefix}/logout`;

  useEffect(() => {
    fetch(`/api/${prefix}/me`, { credentials: 'same-origin' })
      .then(res => res.ok ? res.json() : null)
      .then(data => { setMe(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, [prefix]);

  return html`
    <div class="container" style="padding-top: 1.5rem;">
      <hgroup>
        <h2>${title}</h2>
        <p>${description}</p>
      </hgroup>
      <${ErrorBanner} />
      <${FlowSteps} arch=${arch} />
      ${loading ? html`<p>Checking session...</p>` : ''}
      ${!loading && !me ? notLoggedInContent : ''}
      ${!loading && me ? html`
        <div>
          <div style="display: flex; justify-content: flex-end; margin-bottom: 1rem;">
            <a role="button" class="outline contrast" href=${logout}>Sign out</a>
          </div>
          <${IdentityCard} me=${me} />
          <${ClaimsTable} claims=${me.idTokenClaims} />
          <${TicketsPanel} arch=${arch} apiPrefix=${prefix} userType=${me.userType} />
        </div>
      ` : ''}
    </div>
  `;
}

function Arch1Screen() {
  const notLoggedIn = html`
    <article style="text-align: center; padding: 3rem 1rem;">
      <h3 style="margin-bottom: 1rem;">Not signed in</h3>
      <p style="margin-bottom: 1.5rem;">Authenticate through the central identity broker.</p>
      <a role="button" href="/auth/a1/login" style="padding: 0.75rem 2rem;">Sign in</a>
    </article>
  `;
  return html`
    <${GenericArchScreen}
      arch=${1}
      title="Architecture 1: Central Broker (1 Provider)"
      description="User type is decided by the roles claim in the ID token issued by central broker."
      notLoggedInContent=${notLoggedIn}
    />
  `;
}

function Arch2Screen() {
  const notLoggedIn = html`
    <article style="text-align: center; padding: 3rem 1rem;">
      <h3 style="margin-bottom: 1rem;">Not signed in</h3>
      <p style="margin-bottom: 1.5rem;">Choose your sign-in method to authenticate directly with that provider:</p>
      <div style="display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap;">
        <a role="button" href="/auth/a2/internal/login" style="background-color: #2e7d32; border-color: #2e7d32; padding: 0.75rem 1.5rem;">Sign in as internal</a>
        <a role="button" href="/auth/a2/external/login" style="background-color: #d84315; border-color: #d84315; padding: 0.75rem 1.5rem;">Sign in as external</a>
      </div>
    </article>
  `;
  return html`
    <${GenericArchScreen}
      arch=${2}
      title="Architecture 2: Direct Providers (2 Providers)"
      description="User type is decided by the callback route that completed the flow, confirmed by iss."
      notLoggedInContent=${notLoggedIn}
    />
  `;
}

function Arch3Screen() {
  const port = window.location.port;
  const notLoggedIn = html`
    <article style="text-align: center; padding: 2.5rem 1rem;">
      <h3 style="margin-bottom: 0.5rem;">Not signed in</h3>
      <p style="color: var(--pico-muted-color); margin-bottom: 1.5rem;">
        Current origin: <code>${window.location.origin}</code> ${port === '8083' ? ' (Traefik Internal Gateway)' : port === '8084' ? ' (Traefik External Gateway)' : ' (Direct app port 3000)'}
      </p>
      <div style="display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap; margin-bottom: 1.5rem;">
        <a role="button" href="/auth/a3/internal/login" style="background-color: #2e7d32; border-color: #2e7d32; padding: 0.75rem 1.5rem;">Sign in as internal (kc-internal)</a>
        <a role="button" href="/auth/a3/external/login" style="background-color: #d84315; border-color: #d84315; padding: 0.75rem 1.5rem;">Sign in as external (kc-external)</a>
      </div>
      <p style="font-size: 0.9rem; color: var(--pico-muted-color);">
        Gateway URLs: <a href="http://localhost:8083/architecture-3">Traefik Internal (:8083)</a> · <a href="http://localhost:8084/architecture-3">Traefik External (:8084)</a>
      </p>
    </article>
  `;
  return html`
    <${GenericArchScreen}
      arch=${3}
      title="Architecture 3: 2 Traefiks (Internal 8083 vs External 8084)"
      description="Two network-separated Traefik reverse proxies: internal Traefik routes to kc-internal; external Traefik routes to external email+password login."
      notLoggedInContent=${notLoggedIn}
    />
  `;
}

function Arch4Screen() {
  const port = window.location.port;
  const isInternal = port === '8085';
  const isExternal = port === '8086';

  const notLoggedIn = html`
    <article style="text-align: center; padding: 2.5rem 1rem;">
      <h3 style="margin-bottom: 0.5rem;">Not signed in</h3>
      <p style="color: var(--pico-muted-color); margin-bottom: 1.5rem;">
        Frontend detected port: <strong>${port || 'default'}</strong>
        ${isInternal ? ' → Internal port matched! Redirecting to internal Keycloak.' : ''}
        ${isExternal ? ' → External port matched! Presenting partner login.' : ''}
      </p>
      <div style="display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap; margin-bottom: 1.5rem;">
        ${(!isExternal) ? html`<a role="button" href="/auth/a4/internal/login" style="background-color: #2e7d32; border-color: #2e7d32; padding: 0.75rem 1.5rem;">Sign in as internal</a>` : ''}
        ${(!isInternal) ? html`<a role="button" href="/auth/a4/external/login" style="background-color: #d84315; border-color: #d84315; padding: 0.75rem 1.5rem;">Sign in as external</a>` : ''}
      </div>
      <p style="font-size: 0.9rem; color: var(--pico-muted-color);">
        Traefik Entrypoints: <a href="http://localhost:8085/architecture-4">Internal (:8085)</a> · <a href="http://localhost:8086/architecture-4">External (:8086)</a>
      </p>
    </article>
  `;

  return html`
    <${GenericArchScreen}
      arch=${4}
      title="Architecture 4: 1 Traefik (Frontend Domain/Port Decision)"
      description="Single Traefik with 2 entrypoint ports: Frontend inspects window.location.port to decide whether to trigger internal Keycloak login or show external email+password login."
      notLoggedInContent=${notLoggedIn}
    />
  `;
}

function Arch5Screen({ path, navigate }) {
  const isInternalSubRoute = path.startsWith('/architecture-5/internal');
  const apiPrefix = isInternalSubRoute ? 'a5/internal' : 'a5';
  const logoutPath = isInternalSubRoute ? '/auth/a5/internal/logout' : '/auth/a5/external/logout';
  const loginPath = isInternalSubRoute ? '/auth/a5/internal/login' : '/auth/a5/external/login';

  const notLoggedIn = html`
    <article style="text-align: center; padding: 2.5rem 1rem;">
      <h3 style="margin-bottom: 0.5rem;">${isInternalSubRoute ? 'Internal Portal (/internal)' : 'External Portal (Root)'}</h3>
      <p style="margin-bottom: 1.5rem;">
        ${isInternalSubRoute
          ? 'You are accessing the internal sub-route. Sign in with staff credentials (kc-internal).'
          : 'You are accessing the parent root route. External users sign in with partner credentials (kc-external).'}
      </p>
      <div style="display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap; margin-bottom: 1.5rem;">
        <a role="button" href=${loginPath} style=${isInternalSubRoute ? 'background-color: #2e7d32; border-color: #2e7d32; padding: 0.75rem 1.5rem;' : 'background-color: #d84315; border-color: #d84315; padding: 0.75rem 1.5rem;'}>
          ${isInternalSubRoute ? 'Sign in to Internal Portal' : 'Sign in to External Portal'}
        </a>
      </div>
      <p style="font-size: 0.9rem;">
        ${isInternalSubRoute
          ? html`Switch to <a href="/architecture-5" onClick=${(e) => { e.preventDefault(); navigate('/architecture-5'); }}>External Portal (/architecture-5)</a>`
          : html`Switch to <a href="/architecture-5/internal" onClick=${(e) => { e.preventDefault(); navigate('/architecture-5/internal'); }}>Internal Portal (/architecture-5/internal)</a>`}
      </p>
    </article>
  `;

  return html`
    <${GenericArchScreen}
      arch=${5}
      apiPrefix=${apiPrefix}
      logoutPath=${logoutPath}
      title="Architecture 5: Sub-route Differentiation"
      description="Internal interaction is mounted under /architecture-5/internal; external interaction is mounted under root /architecture-5."
      notLoggedInContent=${notLoggedIn}
    />
  `;
}

function Arch6Screen() {
  const [netInfo, setNetInfo] = useState(null);

  useEffect(() => {
    fetch('/api/a6/network-info')
      .then(r => r.ok ? r.json() : null)
      .then(setNetInfo)
      .catch(() => {});
  }, []);

  const notLoggedIn = html`
    <article style="text-align: center; padding: 2.5rem 1rem;">
      <h3 style="margin-bottom: 0.5rem;">Not signed in</h3>
      <p style="margin-bottom: 1.5rem;">
        Detected Header: <code>X-Network-Zone: ${netInfo?.networkZone || 'evaluating...'}</code>
      </p>
      <div style="display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap; margin-bottom: 1.5rem;">
        <a role="button" href="/auth/a6/login" style="padding: 0.75rem 2rem;">Unified Sign In (/auth/a6/login)</a>
      </div>
      <p style="font-size: 0.9rem; color: var(--pico-muted-color);">
        Test via Traefik: <a href="http://localhost:8083/architecture-6">Internal Traefik (:8083)</a> · <a href="http://localhost:8084/architecture-6">External Traefik (:8084)</a>
      </p>
    </article>
  `;

  return html`
    <${GenericArchScreen}
      arch=${6}
      title="Architecture 6: Backend Header Redirection (X-Network-Zone)"
      description="Traefik injects header X-Network-Zone: internal or external. Backend reads the header and handles redirection automatically at a single unified /auth/a6/login route."
      notLoggedInContent=${notLoggedIn}
    />
  `;
}

function Arch7Screen() {
  const notLoggedIn = html`
    <article style="text-align: center; padding: 2.5rem 1rem;">
      <h3 style="margin-bottom: 0.5rem;">Not signed in</h3>
      <p style="margin-bottom: 1.5rem;">
        Select which provider to hint to the central broker:
      </p>
      <div style="display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap; margin-bottom: 1.5rem;">
        <a role="button" href="/auth/a7/login?hint=internal" style="background-color: #2e7d32; border-color: #2e7d32; padding: 0.75rem 1.5rem;">Sign in with Internal Hint</a>
        <a role="button" href="/auth/a7/login?hint=external" style="background-color: #d84315; border-color: #d84315; padding: 0.75rem 1.5rem;">Sign in with External Hint</a>
      </div>
      <p style="font-size: 0.9rem; color: var(--pico-muted-color);">
        Or let backend auto-detect hint from X-Network-Zone: <a href="/auth/a7/login">Auto Hint Sign In</a>
      </p>
    </article>
  `;

  return html`
    <${GenericArchScreen}
      arch=${7}
      title="Architecture 7: Central Broker with IdP Hint (kc_idp_hint)"
      description="Central Broker issues tokens with mapped roles claim, but the app passes kc_idp_hint to bypass the manual IdP selection screen."
      notLoggedInContent=${notLoggedIn}
    />
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
  } else if (path === '/architecture-3') {
    screen = html`<${Arch3Screen} />`;
  } else if (path === '/architecture-4') {
    screen = html`<${Arch4Screen} />`;
  } else if (path.startsWith('/architecture-5')) {
    screen = html`<${Arch5Screen} path=${path} navigate=${navigate} />`;
  } else if (path === '/architecture-6') {
    screen = html`<${Arch6Screen} />`;
  } else if (path === '/architecture-7') {
    screen = html`<${Arch7Screen} />`;
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
