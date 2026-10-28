import { Router } from 'express';
import { config } from './config.js';
import { startLogin, finishLogin, getLogoutUrl } from './oidc.js';
import { classifyA2 } from './classify.js';
import { upsertUser } from './users.js';
import { getTicketsForUser, addInternalNote } from './tickets.js';

export const arch2Router = Router();

arch2Router.get('/auth/a2/internal/login', async (req, res, next) => {
  try {
    await startLogin(req, res, config.providers['a2-internal']);
  } catch (err) {
    next(err);
  }
});

arch2Router.get('/auth/a2/external/login', async (req, res, next) => {
  try {
    await startLogin(req, res, config.providers['a2-external']);
  } catch (err) {
    next(err);
  }
});

arch2Router.get('/auth/a2/internal/callback', async (req, res, next) => {
  try {
    const { claims, idToken } = await finishLogin(req, config.providers['a2-internal']);
    const { userType, decidedBy, reason } = classifyA2('internal', claims);
    const user = upsertUser({
      iss: claims.iss,
      sub: claims.sub,
      username: claims.preferred_username || claims.sub,
      name: claims.name || claims.preferred_username || claims.sub,
      email: claims.email || '',
      userType
    });
    req.session.a2 = {
      userType,
      decidedBy,
      reason,
      user,
      idToken,
      idTokenClaims: claims,
      loginRoute: '/auth/a2/internal/callback',
      providerKey: 'a2-internal'
    };
    res.redirect('/architecture-2');
  } catch (err) {
    next(err);
  }
});

arch2Router.get('/auth/a2/external/callback', async (req, res, next) => {
  try {
    const { claims, idToken } = await finishLogin(req, config.providers['a2-external']);
    const { userType, decidedBy, reason } = classifyA2('external', claims);
    const user = upsertUser({
      iss: claims.iss,
      sub: claims.sub,
      username: claims.preferred_username || claims.sub,
      name: claims.name || claims.preferred_username || claims.sub,
      email: claims.email || '',
      userType
    });
    req.session.a2 = {
      userType,
      decidedBy,
      reason,
      user,
      idToken,
      idTokenClaims: claims,
      loginRoute: '/auth/a2/external/callback',
      providerKey: 'a2-external'
    };
    res.redirect('/architecture-2');
  } catch (err) {
    next(err);
  }
});

arch2Router.get('/auth/a2/logout', async (req, res, next) => {
  try {
    const sessionA2 = req.session.a2;
    delete req.session.a2;
    const providerKey = sessionA2?.providerKey || 'a2-internal';
    const provider = config.providers[providerKey];
    const logoutUrl = await getLogoutUrl(provider, sessionA2?.idToken, `${config.appBaseUrl}/architecture-2`);
    res.redirect(logoutUrl);
  } catch (err) {
    next(err);
  }
});

arch2Router.get('/api/a2/me', (req, res) => {
  if (!req.session?.a2) {
    return res.status(401).json({ error: 'Not authenticated' });
  }
  const { userType, decidedBy, reason, user, loginRoute, idTokenClaims } = req.session.a2;
  res.json({
    architecture: 2,
    userType,
    decidedBy,
    reason,
    user,
    loginRoute,
    idTokenClaims
  });
});

arch2Router.get('/api/a2/tickets', (req, res) => {
  if (!req.session?.a2) {
    return res.status(401).json({ error: 'Not authenticated' });
  }
  const { userType, user } = req.session.a2;
  res.json(getTicketsForUser(userType, user.email));
});

arch2Router.post('/api/a2/tickets/:id/internal-note', (req, res) => {
  if (!req.session?.a2) {
    return res.status(401).json({ error: 'Not authenticated' });
  }
  if (req.session.a2.userType !== 'internal') {
    return res.status(403).json({ error: 'Forbidden: internal users only' });
  }
  const note = addInternalNote(req.params.id, req.session.a2.user.name, req.body.text);
  if (!note) {
    return res.status(404).json({ error: 'Ticket not found' });
  }
  res.status(201).json(note);
});
