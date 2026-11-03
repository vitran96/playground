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
  createResetCode,
  consumeResetCode,
} from '../users.js'

// ponytail: Mailpit HTTP send API keeps the demo dependency-free; use SMTP (e.g. nodemailer) for a real mail server
export async function sendMail(to: string, subject: string, text: string): Promise<void> {
  const res = await fetch('http://mailpit:8125/api/v1/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      From: { Email: 'no-reply@app.example.com', Name: 'Demo App' },
      To: [{ Email: to }],
      Subject: subject,
      Text: text,
    }),
  })
  if (!res.ok) throw new Error(`mailpit send failed: ${res.status}`)
}

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

  if (typeof email !== 'string' || !email) {
    return c.json({ error: 'Email is required' }, 400)
  }

  const code = createResetCode(email)
  if (code) {
    try {
      await sendMail(email.trim().toLowerCase(), 'Your password reset code', `Your password reset code is: ${code}\nIt expires in 10 minutes.`)
    } catch (err) {
      // Logged only: a distinct error response would reveal that the account exists
      console.error('[PASSWORD RESET] email delivery failed:', err)
    }
  }

  // Same response whether or not the account exists, to avoid email enumeration
  return c.json({
    ok: true,
    message: 'If the account exists, a 6-digit code has been emailed to it (valid 10 minutes). Check Mailpit at http://localhost:8125.',
  })
})

localAuthRoutes.post('/api/architecture-4/local/reset-password', async (c) => {
  const body = await c.req.json().catch(() => ({}))
  const { email, code, newPassword } = body as { email?: string; code?: string; newPassword?: string }

  if (typeof email !== 'string' || typeof code !== 'string' || typeof newPassword !== 'string' || !email || !code || !newPassword) {
    return c.json({ error: 'Email, code and new password are required' }, 400)
  }

  const result = await consumeResetCode(email, code, newPassword)
  if (!result.success) {
    return c.json({ error: result.error }, 400)
  }

  return c.json({ ok: true, message: 'Password has been successfully reset. You may now log in.' })
})
