/**
 * SSO gateway hand-off route.
 * Used by: Architectures 1, 2, 3 (internal & external), and 4 (internal only).
 *
 * Reached only after the Apache gateway has verified OIDC authentication.
 * Reads trustworthy X-RemoteUser* headers (which Apache stripped from the client
 * and populated from verified OIDC tokens), mints the application JWT cookie,
 * and redirects safely to the frontend app.
 */

import { Hono } from 'hono'
import type { GatewayVariables } from '../gateway.js'
import { startSession } from '../session.js'
import { profileName } from '../users.js'

export const ssoRoutes = new Hono<{ Variables: GatewayVariables }>()

// Open-redirect prevention: only permit relative URLs strictly under /${arch}/
function safeNext(next: string | undefined, arch: string): string {
  const defaultPath = `/${arch}/`
  if (!next) return defaultPath

  // Reject URLs containing protocol schemes, double-slashes, or backslashes
  if (next.includes(':') || next.startsWith('//') || next.includes('\\')) {
    return defaultPath
  }

  // Must begin strictly with /${arch}/
  if (next.startsWith(defaultPath)) {
    return next
  }

  return defaultPath
}

// Handles GET /api/:arch/login, /api/:arch/login/internal, and /api/:arch/login/external
ssoRoutes.get('/api/:arch/login/*?', async (c) => {
  const gw = c.get('gw')
  if (!gw.oidc) {
    return c.json({ error: 'this gateway has no SSO login' }, 404)
  }

  const username = c.req.header('X-RemoteUser')
  if (!username) {
    return c.json({ error: 'gateway did not authenticate the user' }, 401)
  }

  const roles = (c.req.header('X-RemoteUserRole') ?? '')
    .split(',')
    .map((r) => r.trim())
    .filter(Boolean)

  const viaKc1 = c.req.header('X-RemoteUserIdp') === 'internal-kc1'
  const userType: 'internal' | 'external' = gw.zone === 'internal' || viaKc1 ? 'internal' : 'external'

  await startSession(c, gw, {
    username,
    roles,
    userType,
    displayName: profileName(username),
  })

  return c.redirect(safeNext(c.req.query('next'), c.req.param('arch')))
})
