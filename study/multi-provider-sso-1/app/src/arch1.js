import { Hono } from 'hono';
import { config } from './config.js';
import { startLogin, finishLogin, getLogoutUrl } from './oidc.js';
import { classifyA1 } from './classify.js';
import { upsertUser } from './users.js';
import { getTicketsForUser, addInternalNote } from './tickets.js';

export const arch1Router = new Hono();

arch1Router.get('/auth/a1/login', (c) => {
  return startLogin(c, config.providers['a1']);
});

arch1Router.get('/auth/a1/callback', async (c) => {
  const { claims, idToken } = await finishLogin(c, config.providers['a1']);
  const { userType, decidedBy, reason } = classifyA1(claims);
  if (!userType) {
    return c.redirect('/architecture-1?error=no_role');
  }
  const user = upsertUser({
    iss: claims.iss,
    sub: claims.sub,
    username: claims.preferred_username || claims.sub,
    name: claims.name || claims.preferred_username || claims.sub,
    email: claims.email || '',
    userType
  });
  const session = c.get('session');
  session.a1 = {
    userType,
    decidedBy,
    reason,
    user,
    idToken,
    idTokenClaims: claims,
    loginRoute: '/auth/a1/callback'
  };
  return c.redirect('/architecture-1');
});

arch1Router.get('/auth/a1/logout', async (c) => {
  const session = c.get('session');
  const idToken = session?.a1?.idToken;
  if (session?.a1) delete session.a1;
  const logoutUrl = await getLogoutUrl(config.providers['a1'], idToken, `${config.appBaseUrl}/architecture-1`);
  return c.redirect(logoutUrl);
});

arch1Router.get('/api/a1/me', (c) => {
  const session = c.get('session');
  if (!session?.a1) {
    return c.json({ error: 'Not authenticated' }, 401);
  }
  const { userType, decidedBy, reason, user, loginRoute, idTokenClaims } = session.a1;
  return c.json({
    architecture: 1,
    userType,
    decidedBy,
    reason,
    user,
    loginRoute,
    idTokenClaims
  });
});

arch1Router.get('/api/a1/tickets', (c) => {
  const session = c.get('session');
  if (!session?.a1) {
    return c.json({ error: 'Not authenticated' }, 401);
  }
  const { userType, user } = session.a1;
  return c.json(getTicketsForUser(userType, user.email));
});

arch1Router.post('/api/a1/tickets/:id/internal-note', async (c) => {
  const session = c.get('session');
  if (!session?.a1) {
    return c.json({ error: 'Not authenticated' }, 401);
  }
  if (session.a1.userType !== 'internal') {
    return c.json({ error: 'Forbidden: internal users only' }, 403);
  }
  const body = await c.req.json();
  const note = addInternalNote(c.req.param('id'), session.a1.user.name, body.text);
  if (!note) {
    return c.json({ error: 'Ticket not found' }, 404);
  }
  return c.json(note, 201);
});
