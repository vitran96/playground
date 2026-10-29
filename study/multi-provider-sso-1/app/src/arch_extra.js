import { Hono } from 'hono';
import { config } from './config.js';
import { startLogin, finishLogin, getLogoutUrl, getAppBase } from './oidc.js';
import { classifyA1, classifyA2 } from './classify.js';
import { upsertUser } from './users.js';
import { getTicketsForUser, addInternalNote } from './tickets.js';

export const archExtraRouter = new Hono();

function setupSession(c, sessionKey, userType, claims, idToken, loginRoute, decidedBy, reason) {
  const user = upsertUser({
    iss: claims.iss,
    sub: claims.sub,
    username: claims.preferred_username || claims.sub,
    name: claims.name || claims.preferred_username || claims.sub,
    email: claims.email || '',
    userType
  });
  const session = c.get('session');
  session[sessionKey] = {
    userType,
    decidedBy,
    reason,
    user,
    idToken,
    idTokenClaims: claims,
    loginRoute
  };
  return session[sessionKey];
}

function mountCrud(router, apiPrefix, sessionKey, archNum) {
  router.get(`/api/${apiPrefix}/me`, (c) => {
    const session = c.get('session');
    if (!session?.[sessionKey]) return c.json({ error: 'Not authenticated' }, 401);
    return c.json({ architecture: archNum, ...session[sessionKey] });
  });

  router.get(`/api/${apiPrefix}/tickets`, (c) => {
    const session = c.get('session');
    if (!session?.[sessionKey]) return c.json({ error: 'Not authenticated' }, 401);
    const { userType, user } = session[sessionKey];
    return c.json(getTicketsForUser(userType, user.email));
  });

  router.post(`/api/${apiPrefix}/tickets/:id/internal-note`, async (c) => {
    const session = c.get('session');
    if (!session?.[sessionKey]) return c.json({ error: 'Not authenticated' }, 401);
    if (session[sessionKey].userType !== 'internal') {
      return c.json({ error: 'Forbidden: internal users only' }, 403);
    }
    const body = await c.req.json();
    const note = addInternalNote(c.req.param('id'), session[sessionKey].user.name, body.text);
    if (!note) return c.json({ error: 'Ticket not found' }, 404);
    return c.json(note, 201);
  });
}

// ---------------------------------------------------------------------
// Architecture 3: 2 Traefiks (Internal 8083, External 8084)
// ---------------------------------------------------------------------
archExtraRouter.get('/auth/a3/internal/login', (c) => {
  return startLogin(c, config.providers['a2-internal'], { callbackPath: '/auth/a3/internal/callback' });
});

archExtraRouter.get('/auth/a3/internal/callback', async (c) => {
  const { claims, idToken } = await finishLogin(c, config.providers['a2-internal'], { callbackPath: '/auth/a3/internal/callback' });
  setupSession(c, 'a3', 'internal', claims, idToken, '/auth/a3/internal/callback', 'gateway: internal traefik', 'connected via traefik-internal (8083) to kc-internal');
  return c.redirect('/architecture-3');
});

archExtraRouter.get('/auth/a3/external/login', (c) => {
  return startLogin(c, config.providers['a2-external'], { callbackPath: '/auth/a3/external/callback' });
});

archExtraRouter.get('/auth/a3/external/callback', async (c) => {
  const { claims, idToken } = await finishLogin(c, config.providers['a2-external'], { callbackPath: '/auth/a3/external/callback' });
  setupSession(c, 'a3', 'external', claims, idToken, '/auth/a3/external/callback', 'gateway: external traefik', 'connected via traefik-external (8084) to kc-external');
  return c.redirect('/architecture-3');
});

archExtraRouter.get('/auth/a3/logout', (c) => {
  if (c.get('session')?.a3) delete c.get('session').a3;
  return c.redirect('/architecture-3');
});

mountCrud(archExtraRouter, 'a3', 'a3', 3);

// ---------------------------------------------------------------------
// Architecture 4: 1 Traefik (Domain/Port Decided by Frontend: 8085 vs 8086)
// ---------------------------------------------------------------------
archExtraRouter.get('/auth/a4/internal/login', (c) => {
  return startLogin(c, config.providers['a2-internal'], { callbackPath: '/auth/a4/internal/callback' });
});

archExtraRouter.get('/auth/a4/internal/callback', async (c) => {
  const { claims, idToken } = await finishLogin(c, config.providers['a2-internal'], { callbackPath: '/auth/a4/internal/callback' });
  setupSession(c, 'a4', 'internal', claims, idToken, '/auth/a4/internal/callback', 'frontend port/domain check (port 8085)', 'FE detected internal port 8085 and redirected to kc-internal');
  return c.redirect('/architecture-4');
});

archExtraRouter.get('/auth/a4/external/login', (c) => {
  return startLogin(c, config.providers['a2-external'], { callbackPath: '/auth/a4/external/callback' });
});

archExtraRouter.get('/auth/a4/external/callback', async (c) => {
  const { claims, idToken } = await finishLogin(c, config.providers['a2-external'], { callbackPath: '/auth/a4/external/callback' });
  setupSession(c, 'a4', 'external', claims, idToken, '/auth/a4/external/callback', 'frontend port/domain check (port 8086)', 'FE detected external port 8086 and rendered email+pw login');
  return c.redirect('/architecture-4');
});

archExtraRouter.get('/auth/a4/logout', (c) => {
  if (c.get('session')?.a4) delete c.get('session').a4;
  return c.redirect('/architecture-4');
});

mountCrud(archExtraRouter, 'a4', 'a4', 4);

// ---------------------------------------------------------------------
// Architecture 5: Sub-route Differentiation (/internal vs parent root)
// ---------------------------------------------------------------------
archExtraRouter.get('/auth/a5/internal/login', (c) => {
  return startLogin(c, config.providers['a2-internal'], { callbackPath: '/auth/a5/internal/callback' });
});

archExtraRouter.get('/auth/a5/internal/callback', async (c) => {
  const { claims, idToken } = await finishLogin(c, config.providers['a2-internal'], { callbackPath: '/auth/a5/internal/callback' });
  setupSession(c, 'a5_internal', 'internal', claims, idToken, '/auth/a5/internal/callback', 'sub-route /internal', 'accessed and mounted under /architecture-5/internal');
  return c.redirect('/architecture-5/internal');
});

archExtraRouter.get('/auth/a5/internal/logout', (c) => {
  if (c.get('session')?.a5_internal) delete c.get('session').a5_internal;
  return c.redirect('/architecture-5/internal');
});

archExtraRouter.get('/auth/a5/external/login', (c) => {
  return startLogin(c, config.providers['a2-external'], { callbackPath: '/auth/a5/external/callback' });
});

archExtraRouter.get('/auth/a5/external/callback', async (c) => {
  const { claims, idToken } = await finishLogin(c, config.providers['a2-external'], { callbackPath: '/auth/a5/external/callback' });
  setupSession(c, 'a5_external', 'external', claims, idToken, '/auth/a5/external/callback', 'parent root route', 'accessed and mounted under root /architecture-5');
  return c.redirect('/architecture-5');
});

archExtraRouter.get('/auth/a5/external/logout', (c) => {
  if (c.get('session')?.a5_external) delete c.get('session').a5_external;
  return c.redirect('/architecture-5');
});

mountCrud(archExtraRouter, 'a5/internal', 'a5_internal', 5);
mountCrud(archExtraRouter, 'a5', 'a5_external', 5);

// ---------------------------------------------------------------------
// Architecture 6: Backend Handles Redirection via Header X-Network-Zone
// ---------------------------------------------------------------------
archExtraRouter.get('/api/a6/network-info', (c) => {
  return c.json({
    networkZone: c.req.header('x-network-zone') || 'none (direct)',
    host: c.req.header('host')
  });
});

archExtraRouter.get('/auth/a6/login', (c) => {
  const zone = c.req.header('x-network-zone') || c.req.query('zone') || 'internal';
  if (zone === 'external') {
    return startLogin(c, config.providers['a2-external'], { callbackPath: '/auth/a6/callback' });
  }
  return startLogin(c, config.providers['a2-internal'], { callbackPath: '/auth/a6/callback' });
});

archExtraRouter.get('/auth/a6/callback', async (c) => {
  const session = c.get('session');
  const isExt = !!session?.pending?.['a2-external'];
  const provider = isExt ? config.providers['a2-external'] : config.providers['a2-internal'];
  const userType = isExt ? 'external' : 'internal';
  const { claims, idToken } = await finishLogin(c, provider, { callbackPath: '/auth/a6/callback' });
  setupSession(c, 'a6', userType, claims, idToken, '/auth/a6/callback', 'backend header evaluation', `backend evaluated X-Network-Zone: ${userType}`);
  return c.redirect('/architecture-6');
});

archExtraRouter.get('/auth/a6/logout', (c) => {
  if (c.get('session')?.a6) delete c.get('session').a6;
  return c.redirect('/architecture-6');
});

mountCrud(archExtraRouter, 'a6', 'a6', 6);

// ---------------------------------------------------------------------
// Architecture 7: Central Broker with IdP Hint (kc_idp_hint)
// ---------------------------------------------------------------------
archExtraRouter.get('/auth/a7/login', (c) => {
  const zoneHeader = c.req.header('x-network-zone');
  const hint = c.req.query('hint') || (zoneHeader === 'external' ? 'external' : 'internal');
  return startLogin(c, config.providers['a1'], {
    callbackPath: '/auth/a7/callback',
    kc_idp_hint: hint
  });
});

archExtraRouter.get('/auth/a7/callback', async (c) => {
  const { claims, idToken } = await finishLogin(c, config.providers['a1'], { callbackPath: '/auth/a7/callback' });
  const { userType, reason } = classifyA1(claims);
  if (!userType) return c.redirect('/architecture-7?error=no_role');
  setupSession(c, 'a7', userType, claims, idToken, '/auth/a7/callback', 'central broker (roles claim) + kc_idp_hint', `${reason}; auto-selected IdP via kc_idp_hint=${claims.idp || 'hint'}`);
  return c.redirect('/architecture-7');
});

archExtraRouter.get('/auth/a7/logout', (c) => {
  if (c.get('session')?.a7) delete c.get('session').a7;
  return c.redirect('/architecture-7');
});

mountCrud(archExtraRouter, 'a7', 'a7', 7);
