import { Hono } from 'hono';
import { config } from './config.js';
import { startLogin, finishLogin, getLogoutUrl } from './oidc.js';
import { classifyA2 } from './classify.js';
import { upsertUser } from './users.js';
import { getTicketsForUser, addInternalNote } from './tickets.js';

export const arch2Router = new Hono();

arch2Router.get('/auth/a2/internal/login', (c) => {
  return startLogin(c, config.providers['a2-internal']);
});

arch2Router.get('/auth/a2/external/login', (c) => {
  return startLogin(c, config.providers['a2-external']);
});

arch2Router.get('/auth/a2/internal/callback', async (c) => {
  const { claims, idToken } = await finishLogin(c, config.providers['a2-internal']);
  const { userType, decidedBy, reason } = classifyA2('internal', claims);
  const user = upsertUser({
    iss: claims.iss,
    sub: claims.sub,
    username: claims.preferred_username || claims.sub,
    name: claims.name || claims.preferred_username || claims.sub,
    email: claims.email || '',
    userType
  });
  const session = c.get('session');
  session.a2 = {
    userType,
    decidedBy,
    reason,
    user,
    idToken,
    idTokenClaims: claims,
    loginRoute: '/auth/a2/internal/callback',
    providerKey: 'a2-internal'
  };
  return c.redirect('/architecture-2');
});

arch2Router.get('/auth/a2/external/callback', async (c) => {
  const { claims, idToken } = await finishLogin(c, config.providers['a2-external']);
  const { userType, decidedBy, reason } = classifyA2('external', claims);
  const user = upsertUser({
    iss: claims.iss,
    sub: claims.sub,
    username: claims.preferred_username || claims.sub,
    name: claims.name || claims.preferred_username || claims.sub,
    email: claims.email || '',
    userType
  });
  const session = c.get('session');
  session.a2 = {
    userType,
    decidedBy,
    reason,
    user,
    idToken,
    idTokenClaims: claims,
    loginRoute: '/auth/a2/external/callback',
    providerKey: 'a2-external'
  };
  return c.redirect('/architecture-2');
});

arch2Router.get('/auth/a2/logout', async (c) => {
  const session = c.get('session');
  const sessionA2 = session?.a2;
  if (session?.a2) delete session.a2;
  const providerKey = sessionA2?.providerKey || 'a2-internal';
  const provider = config.providers[providerKey];
  const logoutUrl = await getLogoutUrl(provider, sessionA2?.idToken, `${config.appBaseUrl}/architecture-2`);
  return c.redirect(logoutUrl);
});

arch2Router.get('/api/a2/me', (c) => {
  const session = c.get('session');
  if (!session?.a2) {
    return c.json({ error: 'Not authenticated' }, 401);
  }
  const { userType, decidedBy, reason, user, loginRoute, idTokenClaims } = session.a2;
  return c.json({
    architecture: 2,
    userType,
    decidedBy,
    reason,
    user,
    loginRoute,
    idTokenClaims
  });
});

arch2Router.get('/api/a2/tickets', (c) => {
  const session = c.get('session');
  if (!session?.a2) {
    return c.json({ error: 'Not authenticated' }, 401);
  }
  const { userType, user } = session.a2;
  return c.json(getTicketsForUser(userType, user.email));
});

arch2Router.post('/api/a2/tickets/:id/internal-note', async (c) => {
  const session = c.get('session');
  if (!session?.a2) {
    return c.json({ error: 'Not authenticated' }, 401);
  }
  if (session.a2.userType !== 'internal') {
    return c.json({ error: 'Forbidden: internal users only' }, 403);
  }
  const body = await c.req.json();
  const note = addInternalNote(c.req.param('id'), session.a2.user.name, body.text);
  if (!note) {
    return c.json({ error: 'Ticket not found' }, 404);
  }
  return c.json(note, 201);
});
