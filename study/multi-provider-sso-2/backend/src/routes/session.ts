/**
 * Session management endpoints.
 * Used by: Architectures 1, 2, 3, 4.
 *
 * Implements /gateway, /me, /hello (sample protected API), and /logout.
 */

import { Hono } from 'hono'
import type { GatewayVariables } from '../gateway.js'
import { readSession, clearSession } from '../session.js'

export const sessionRoutes = new Hono<{ Variables: GatewayVariables }>()

// Returns gateway metadata so frontend knows whether to redirect, show IdP buttons, or show login form
sessionRoutes.get('/api/:arch/gateway', (c) => {
  const gw = c.get('gw')
  return c.json({
    arch: gw.arch,
    zone: gw.zone,
    loginUi: gw.loginUi,
  })
})

// Current user profile and decoded JWT claims (or 401)
sessionRoutes.get('/api/:arch/me', async (c) => {
  const gw = c.get('gw')
  const claims = await readSession(c, gw)
  if (!claims) {
    return c.json({ error: 'unauthorized' }, 401)
  }

  return c.json({
    user: {
      username: claims.sub,
      displayName: claims.name,
      roles: claims.roles,
      userType: claims.user_type,
    },
    claims,
  })
})

// Sample protected API demonstrating role-based responses
sessionRoutes.get('/api/:arch/hello', async (c) => {
  const gw = c.get('gw')
  const claims = await readSession(c, gw)
  if (!claims) {
    return c.json({ error: 'unauthorized' }, 401)
  }

  const isAdmin = Array.isArray(claims.roles) && claims.roles.includes('app-admin')
  return c.json({
    message: `Hello ${claims.name}! Welcome to Architecture ${gw.arch} (${gw.zone} zone).`,
    admin: isAdmin,
    note: isAdmin ? 'You have administrative privileges.' : 'Standard user privileges.',
    timestamp: new Date().toISOString(),
  })
})

// Logout clears the backend JWT session cookie and signals if gateway OIDC session logout is needed
sessionRoutes.post('/api/:arch/logout', (c) => {
  const gw = c.get('gw')
  clearSession(c, gw)
  return c.json({
    ok: true,
    gatewayLogout: gw.oidc,
  })
})
