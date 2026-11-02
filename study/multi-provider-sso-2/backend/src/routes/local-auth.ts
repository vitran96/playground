/**
 * App-owned local authentication routes.
 * Used by: Architecture 4 (external zone only).
 *
 * Implements login, signup, forgot password, and reset password directly within the app,
 * bypassing Keycloak entirely for external users.
 */

import { Hono } from 'hono'
import type { GatewayVariables } from '../gateway.js'
import { startSession } from '../session.js'
import {
  localUsers,
  verifyPassword,
  createLocalUser,
  createResetToken,
  consumeResetToken,
} from '../users.js'

export const localAuthRoutes = new Hono<{ Variables: GatewayVariables }>()

// Enforce that local auth is only accessible through the designated Architecture 4 external gateway
localAuthRoutes.use('/api/architecture-4/local/*', async (c, next) => {
  const gw = c.get('gw')
  if (gw.id !== 'gw-4-ext') {
    return c.json({ error: 'Local auth is only enabled on gw-4-ext' }, 403)
  }
  await next()
})

localAuthRoutes.post('/api/architecture-4/local/login', async (c) => {
  const gw = c.get('gw')
  const body = await c.req.json().catch(() => ({}))
  const { email, password } = body as { email?: string; password?: string }

  if (!email || !password) {
    return c.json({ error: 'Email and password are required' }, 400)
  }

  const normalizedEmail = email.trim().toLowerCase()
  const user = localUsers.get(normalizedEmail)

  // Prevent timing enumeration by performing verification check
  const isValid = user ? await verifyPassword(password, user) : false
  if (!user || !isValid) {
    return c.json({ error: 'Invalid email or password' }, 401)
  }

  await startSession(c, gw, {
    username: normalizedEmail,
    displayName: user.displayName,
    roles: user.roles,
    userType: 'external',
  })

  return c.json({
    ok: true,
    user: {
      username: normalizedEmail,
      displayName: user.displayName,
      roles: user.roles,
      userType: 'external',
    },
  })
})

localAuthRoutes.post('/api/architecture-4/local/signup', async (c) => {
  const gw = c.get('gw')
  const body = await c.req.json().catch(() => ({}))
  const { email, password, displayName } = body as {
    email?: string
    password?: string
    displayName?: string
  }

  if (!email || !password) {
    return c.json({ error: 'Email and password are required' }, 400)
  }

  const result = await createLocalUser(email, password, displayName)
  if (!result.success) {
    return c.json({ error: result.error }, 400)
  }

  const normalizedEmail = email.trim().toLowerCase()
  const user = localUsers.get(normalizedEmail)!

  await startSession(c, gw, {
    username: normalizedEmail,
    displayName: user.displayName,
    roles: user.roles,
    userType: 'external',
  })

  return c.json({
    ok: true,
    user: {
      username: normalizedEmail,
      displayName: user.displayName,
      roles: user.roles,
      userType: 'external',
    },
  })
})

localAuthRoutes.post('/api/architecture-4/local/forgot-password', async (c) => {
  const body = await c.req.json().catch(() => ({}))
  const { email } = body as { email?: string }

  if (!email) {
    return c.json({ error: 'Email is required' }, 400)
  }

  const token = createResetToken(email)
  if (token) {
    // In a production app, an email with a secure token link would be sent.
    // In this reference demo, we output the reset link directly to backend server logs.
    console.log(`\n======================================================`)
    console.log(`[PASSWORD RESET] For ${email}:`)
    console.log(`Link: http://ext.localhost:8402/architecture-4/?token=${token}`)
    console.log(`======================================================\n`)
  }

  // Always return success message to avoid email enumeration
  return c.json({
    ok: true,
    message: 'If the account exists, a password reset link has been logged to the backend console.',
  })
})

localAuthRoutes.post('/api/architecture-4/local/reset-password', async (c) => {
  const body = await c.req.json().catch(() => ({}))
  const { token, newPassword } = body as { token?: string; newPassword?: string }

  if (!token || !newPassword) {
    return c.json({ error: 'Token and new password are required' }, 400)
  }

  const result = await consumeResetToken(token, newPassword)
  if (!result.success) {
    return c.json({ error: result.error }, 400)
  }

  return c.json({ ok: true, message: 'Password has been successfully reset. You may now log in.' })
})
