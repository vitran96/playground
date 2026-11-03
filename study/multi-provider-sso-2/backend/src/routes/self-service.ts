/**
 * App-owned self-service for Keycloak-backed external users.
 * Used by: Architecture 6 (registration) and Architecture 7 (registration + OTP password reset),
 * external zone only. Sign-in itself stays in keycloak-2 via the gateway.
 */

import crypto from 'node:crypto'
import { Hono } from 'hono'
import type { GatewayVariables } from '../gateway.js'
import { EXTERNAL_KC, createKeycloakUser, kcAdmin } from './admin.js'
import { sendMail } from './local-auth.js'
import { externalEmailError, createResetCode, checkResetCode } from '../users.js'

export const selfServiceRoutes = new Hono<{ Variables: GatewayVariables }>()

// Reset codes share the Arch 4 store; prefix keeps a code issued for an app-local account from resetting a Keycloak one
const codeKey = (email: string) => `kc2:${email}`

selfServiceRoutes.use('/api/:arch/self/*', async (c, next) => {
  const gw = c.get('gw')
  const isRegister = c.req.path.endsWith('/self/register')
  if (gw.zone !== 'external' || !(gw.arch === 7 || (gw.arch === 6 && isRegister))) {
    return c.json({ error: 'Not available on this gateway' }, 403)
  }
  // JSON-only: a cross-site form post cannot send this content type without a CORS preflight
  if (!c.req.header('content-type')?.startsWith('application/json')) {
    return c.json({ error: 'Content-Type must be application/json' }, 415)
  }
  await next()
})

async function findKcUser(arch: number, email: string) {
  const api = await kcAdmin(EXTERNAL_KC[arch])
  if ('error' in api) throw new Error(api.error)
  const res = await fetch(`${api.admin}/users?exact=true&email=${encodeURIComponent(email)}`, { headers: api.headers })
  if (!res.ok) throw new Error(`Keycloak user lookup failed (${res.status})`)
  const [user] = (await res.json()) as { id: string }[]
  return { api, id: user?.id }
}

selfServiceRoutes.post('/api/:arch/self/register', async (c) => {
  const gw = c.get('gw')
  const { email, firstName, lastName } = (await c.req.json().catch(() => ({}))) as Record<string, unknown>
  if (typeof email !== 'string' || typeof firstName !== 'string' || typeof lastName !== 'string') {
    return c.json({ error: 'email, firstName and lastName are required' }, 400)
  }
  const normalizedEmail = email.trim().toLowerCase()
  const first = firstName.trim().slice(0, 100)
  const last = lastName.trim().slice(0, 100)
  const emailError = externalEmailError(normalizedEmail)
  if (emailError) return c.json({ error: emailError }, 400)
  if (!first || !last) return c.json({ error: 'First and last name are required' }, 400)

  // Temporary: Keycloak forces the user to choose a new password at first sign-in, so the emailed one is single-use
  const password = crypto.randomBytes(12).toString('base64url')
  const result = await createKeycloakUser(EXTERNAL_KC[gw.arch], normalizedEmail, first, last, password, true).catch((err) => {
    console.error('[SELF REGISTER] Keycloak call failed:', err)
    return { status: 502, error: 'Could not reach Keycloak' }
  })
  if (result.error) return c.json({ error: result.error }, result.status as 400)

  try {
    await sendMail(
      normalizedEmail,
      'Your new account',
      `Hello ${first},\n\nYour account has been created.\nEmail: ${normalizedEmail}\nTemporary password: ${password}\n\nYou will be asked to choose a new password when you first sign in.`
    )
  } catch (err) {
    console.error('[SELF REGISTER] email delivery failed:', err)
    return c.json({ error: 'Account created, but the password email could not be sent. Use "Forgot password" to set one.' }, 502)
  }

  return c.json({ ok: true, message: `Account created. A temporary password was emailed to ${normalizedEmail} (see Mailpit at http://localhost:8125). Click Sign In to use it.` }, 201)
})

selfServiceRoutes.post('/api/:arch/self/forgot-password', async (c) => {
  const gw = c.get('gw')
  const { email } = (await c.req.json().catch(() => ({}))) as { email?: unknown }
  if (typeof email !== 'string' || !email) return c.json({ error: 'Email is required' }, 400)
  const normalizedEmail = email.trim().toLowerCase()

  try {
    const { id } = await findKcUser(gw.arch, normalizedEmail)
    const code = createResetCode(codeKey(normalizedEmail), !!id)
    if (code) await sendMail(normalizedEmail, 'Your password reset code', `Your password reset code is: ${code}\nIt expires in 10 minutes.`)
  } catch (err) {
    // Logged only: a distinct error response would reveal that the account exists
    console.error('[SELF RESET] request failed:', err)
  }

  return c.json({
    ok: true,
    message: 'If the account exists, a 6-digit code has been emailed to it (valid 10 minutes). Check Mailpit at http://localhost:8125.',
  })
})

selfServiceRoutes.post('/api/:arch/self/reset-password', async (c) => {
  const gw = c.get('gw')
  const { email, code, newPassword } = (await c.req.json().catch(() => ({}))) as Record<string, unknown>
  if (typeof email !== 'string' || typeof code !== 'string' || typeof newPassword !== 'string' || !email || !code || !newPassword) {
    return c.json({ error: 'Email, code and new password are required' }, 400)
  }
  if (newPassword.length < 8) return c.json({ error: 'Password must be at least 8 characters long' }, 400)
  const normalizedEmail = email.trim().toLowerCase()
  const invalid = () => c.json({ error: 'Invalid or expired reset code' }, 400)

  if (!checkResetCode(codeKey(normalizedEmail), code)) return invalid()

  try {
    const { api, id } = await findKcUser(gw.arch, normalizedEmail)
    if (!id) return invalid()
    const res = await fetch(`${api.admin}/users/${id}/reset-password`, {
      method: 'PUT',
      headers: api.headers,
      body: JSON.stringify({ type: 'password', value: newPassword, temporary: false }),
    })
    if (!res.ok) {
      console.error('[SELF RESET] Keycloak reset-password failed:', res.status, await res.text())
      return c.json({ error: 'Keycloak rejected the new password' }, 400)
    }
  } catch (err) {
    console.error('[SELF RESET] Keycloak call failed:', err)
    return c.json({ error: 'Could not reach Keycloak' }, 502)
  }

  return c.json({ ok: true, message: 'Password has been reset. Click Sign In to use it.' })
})
