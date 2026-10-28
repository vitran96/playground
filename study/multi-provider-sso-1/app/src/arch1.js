import { Router } from 'express';
import { config } from './config.js';
import { startLogin, finishLogin, getLogoutUrl } from './oidc.js';
import { classifyA1 } from './classify.js';
import { upsertUser } from './users.js';
import { getTicketsForUser, addInternalNote } from './tickets.js';

export const arch1Router = Router();

arch1Router.get('/auth/a1/login', async (req, res, next) => {
  try {
    await startLogin(req, res, config.providers['a1']);
  } catch (err) {
    next(err);
  }
});

arch1Router.get('/auth/a1/callback', async (req, res, next) => {
  try {
    const { claims, idToken } = await finishLogin(req, config.providers['a1']);
    const { userType, decidedBy, reason } = classifyA1(claims);
    if (!userType) {
      return res.redirect('/architecture-1?error=no_role');
    }
    const user = upsertUser({
      iss: claims.iss,
      sub: claims.sub,
      username: claims.preferred_username || claims.sub,
      name: claims.name || claims.preferred_username || claims.sub,
      email: claims.email || '',
      userType
    });
    req.session.a1 = {
      userType,
      decidedBy,
      reason,
      user,
      idToken,
      idTokenClaims: claims,
      loginRoute: '/auth/a1/callback'
    };
    res.redirect('/architecture-1');
  } catch (err) {
    next(err);
  }
});

arch1Router.get('/auth/a1/logout', async (req, res, next) => {
  try {
    const idToken = req.session.a1?.idToken;
    delete req.session.a1;
    const logoutUrl = await getLogoutUrl(config.providers['a1'], idToken, `${config.appBaseUrl}/architecture-1`);
    res.redirect(logoutUrl);
  } catch (err) {
    next(err);
  }
});

arch1Router.get('/api/a1/me', (req, res) => {
  if (!req.session?.a1) {
    return res.status(401).json({ error: 'Not authenticated' });
  }
  const { userType, decidedBy, reason, user, loginRoute, idTokenClaims } = req.session.a1;
  res.json({
    architecture: 1,
    userType,
    decidedBy,
    reason,
    user,
    loginRoute,
    idTokenClaims
  });
});

arch1Router.get('/api/a1/tickets', (req, res) => {
  if (!req.session?.a1) {
    return res.status(401).json({ error: 'Not authenticated' });
  }
  const { userType, user } = req.session.a1;
  res.json(getTicketsForUser(userType, user.email));
});

arch1Router.post('/api/a1/tickets/:id/internal-note', (req, res) => {
  if (!req.session?.a1) {
    return res.status(401).json({ error: 'Not authenticated' });
  }
  if (req.session.a1.userType !== 'internal') {
    return res.status(403).json({ error: 'Forbidden: internal users only' });
  }
  const note = addInternalNote(req.params.id, req.session.a1.user.name, req.body.text);
  if (!note) {
    return res.status(404).json({ error: 'Ticket not found' });
  }
  res.status(201).json(note);
});
