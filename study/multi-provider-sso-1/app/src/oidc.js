import crypto from 'node:crypto';
import { jwtVerify, createRemoteJWKSet } from 'jose';

const cache = new Map();

export async function getDiscovery(provider) {
  if (cache.has(provider.key)) {
    return cache.get(provider.key);
  }
  const wellKnown = `${provider.backchannel.replace(/\/$/, '')}/.well-known/openid-configuration`;
  const res = await fetch(wellKnown);
  if (!res.ok) {
    throw new Error(`OIDC discovery failed for ${provider.key}: ${res.status} ${await res.text()}`);
  }
  const metadata = await res.json();
  if (metadata.issuer !== provider.issuer) {
    throw new Error(`Issuer mismatch for ${provider.key}: expected ${provider.issuer}, got ${metadata.issuer}`);
  }

  const issuerOrigin = new URL(provider.issuer).origin;
  const backchannelOrigin = new URL(provider.backchannel).origin;
  const tokenEndpoint = metadata.token_endpoint.replace(issuerOrigin, backchannelOrigin);
  const jwksUri = metadata.jwks_uri.replace(issuerOrigin, backchannelOrigin);

  const discovery = {
    issuer: metadata.issuer,
    authorization_endpoint: metadata.authorization_endpoint,
    end_session_endpoint: metadata.end_session_endpoint || `${metadata.issuer}/protocol/openid-connect/logout`,
    token_endpoint: tokenEndpoint,
    jwks_uri: jwksUri,
    jwks: createRemoteJWKSet(new URL(jwksUri))
  };

  cache.set(provider.key, discovery);
  return discovery;
}

export async function startLogin(req, res, provider) {
  const discovery = await getDiscovery(provider);
  const state = crypto.randomBytes(16).toString('hex');
  const nonce = crypto.randomBytes(16).toString('hex');
  const verifier = crypto.randomBytes(32).toString('base64url');
  const challenge = crypto.createHash('sha256').update(verifier).digest('base64url');

  req.session.pending = req.session.pending || {};
  req.session.pending[provider.key] = { state, nonce, verifier };

  const authUrl = new URL(discovery.authorization_endpoint);
  authUrl.searchParams.set('response_type', 'code');
  authUrl.searchParams.set('client_id', provider.clientId);
  authUrl.searchParams.set('redirect_uri', provider.redirectUri);
  authUrl.searchParams.set('scope', 'openid profile email');
  authUrl.searchParams.set('state', state);
  authUrl.searchParams.set('nonce', nonce);
  authUrl.searchParams.set('code_challenge', challenge);
  authUrl.searchParams.set('code_challenge_method', 'S256');

  return res.redirect(authUrl.toString());
}

export async function finishLogin(req, provider) {
  const pending = req.session.pending?.[provider.key];
  if (!pending) {
    const err = new Error('no login in progress for this provider');
    err.status = 400;
    throw err;
  }

  if (req.query.state !== pending.state) {
    const err = new Error('state mismatch');
    err.status = 400;
    throw err;
  }

  delete req.session.pending[provider.key];

  if (req.query.error) {
    const err = new Error(req.query.error_description || req.query.error);
    err.status = 400;
    throw err;
  }

  if (!req.query.code) {
    const err = new Error('missing authorization code');
    err.status = 400;
    throw err;
  }

  const discovery = await getDiscovery(provider);
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    code: req.query.code,
    redirect_uri: provider.redirectUri,
    client_id: provider.clientId,
    client_secret: provider.clientSecret,
    code_verifier: pending.verifier
  });

  const tokenRes = await fetch(discovery.token_endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body
  });

  if (!tokenRes.ok) {
    const errText = await tokenRes.text();
    const err = new Error(`Token exchange failed: ${tokenRes.status} ${errText}`);
    err.status = 502;
    throw err;
  }

  const tokenData = await tokenRes.json();
  const idToken = tokenData.id_token;
  if (!idToken) {
    const err = new Error('No id_token in token response');
    err.status = 502;
    throw err;
  }

  const { payload } = await jwtVerify(idToken, discovery.jwks, {
    issuer: provider.issuer,
    audience: provider.clientId
  });

  if (payload.nonce !== pending.nonce) {
    const err = new Error('nonce mismatch');
    err.status = 400;
    throw err;
  }

  return { claims: payload, idToken };
}

export async function getLogoutUrl(provider, idToken, postLogoutRedirectUri) {
  const discovery = await getDiscovery(provider);
  const url = new URL(discovery.end_session_endpoint);
  if (idToken) {
    url.searchParams.set('id_token_hint', idToken);
  }
  if (postLogoutRedirectUri) {
    url.searchParams.set('post_logout_redirect_uri', postLogoutRedirectUri);
  }
  return url.toString();
}
