/**
 * App-admin user registration.
 * Used by: Architectures 1, 2, 3 (creates the user in that architecture's external Keycloak)
 * and Architecture 4 (creates the user in the app's own local store).
 *
 * Only external users can be registered here; internal users come from the company directory.
 */

import { Hono } from 'hono'
import type { GatewayVariables } from '../gateway.js'
import { readSession } from '../session.js'
import { createLocalUser, externalEmailError } from '../users.js'

export const adminRoutes = new Hono<{ Variables: GatewayVariables }>()

const DEFAULT_PASSWORD = 'password'

// External identity store per architecture (Arch 4 external users live in the app itself)
const KC2 = { url: 'http://keycloak-2:8180', realm: 'external', label: 'keycloak-2 (realm external)', secretEnv: 'KC2_ADMIN_SECRET' }
export const EXTERNAL_KC: Record<number, { url: string; realm: string; label: string; secretEnv: string }> = {
  1: { url: 'http://keycloak-3:8180', realm: 'arch1', label: 'keycloak-3 (realm arch1)', secretEnv: 'KC3_ADMIN_SECRET' },
  2: { url: 'http://keycloak-4:8180', realm: 'arch2', label: 'keycloak-4 (realm arch2)', secretEnv: 'KC4_ADMIN_SECRET' },
  3: KC2,
  6: KC2,
  7: KC2,
}
type Kc = (typeof EXTERNAL_KC)[number]

// Admin REST base URL + bearer headers for the backend-admin service account
export async function kcAdmin(kc: Kc): Promise<{ admin: string; headers: Record<string, string> } | { status: number; error: string }> {
  const secret = process.env[kc.secretEnv]
  if (!secret) return { status: 500, error: `${kc.secretEnv} is not configured on the backend` }

  const tokenRes = await fetch(`${kc.url}/realms/${kc.realm}/protocol/openid-connect/token`, {
    method: 'POST',
    body: new URLSearchParams({ grant_type: 'client_credentials', client_id: 'backend-admin', client_secret: secret }),
  })
  if (!tokenRes.ok) return { status: 502, error: `Keycloak token request failed (${tokenRes.status})` }
  const { access_token } = (await tokenRes.json()) as { access_token: string }
  return {
    admin: `${kc.url}/admin/realms/${kc.realm}`,
    headers: { Authorization: `Bearer ${access_token}`, 'Content-Type': 'application/json' },
  }
}

export async function createKeycloakUser(
  kc: Kc,
  email: string,
  firstName: string,
  lastName: string,
  password = DEFAULT_PASSWORD,
  temporary = false
): Promise<{ status: number; error?: string }> {
  const api = await kcAdmin(kc)
  if ('error' in api) return api
  const { admin, headers } = api

  const createRes = await fetch(`${admin}/users`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      username: email,
      email,
      firstName,
      lastName,
      enabled: true,
      emailVerified: true,
      credentials: [{ type: 'password', value: password, temporary }],
    }),
  })
  if (createRes.status === 409) return { status: 409, error: 'A user with this email already exists' }
  if (!createRes.ok) return { status: 502, error: `Keycloak user creation failed (${createRes.status})` }
  const userId = createRes.headers.get('location')?.split('/').pop()

  const roleRes = await fetch(`${admin}/roles/partner`, { headers })
  const mapRes = roleRes.ok
    ? await fetch(`${admin}/users/${userId}/role-mappings/realm`, {
        method: 'POST',
        headers,
        body: JSON.stringify([await roleRes.json()]),
      })
    : roleRes
  if (!mapRes.ok) return { status: 502, error: `User created, but assigning the partner role failed (${mapRes.status})` }

  return { status: 201 }
}

adminRoutes.post('/api/:arch/admin/users', async (c) => {
  const gw = c.get('gw')
  const claims = await readSession(c, gw)
  if (!claims) return c.json({ error: 'unauthorized' }, 401)
  if (!claims.roles?.includes('app-admin')) return c.json({ error: 'app-admin role required' }, 403)
  // JSON-only: a cross-site form post cannot send this content type without a CORS preflight
  if (!c.req.header('content-type')?.startsWith('application/json')) {
    return c.json({ error: 'Content-Type must be application/json' }, 415)
  }

  const body = await c.req.json().catch(() => ({}))
  const { email, firstName, lastName } = body as Record<string, unknown>
  if (typeof email !== 'string' || typeof firstName !== 'string' || typeof lastName !== 'string') {
    return c.json({ error: 'email, firstName and lastName are required' }, 400)
  }
  const normalizedEmail = email.trim().toLowerCase()
  const first = firstName.trim().slice(0, 100)
  const last = lastName.trim().slice(0, 100)
  const emailError = externalEmailError(normalizedEmail)
  if (emailError) return c.json({ error: emailError }, 400)
  if (!first || !last) return c.json({ error: 'First and last name are required' }, 400)

  if (gw.arch === 4) {
    const result = await createLocalUser(normalizedEmail, DEFAULT_PASSWORD, `${first} ${last}`)
    if (!result.success) return c.json({ error: result.error }, 400)
    return c.json({ ok: true, message: `Created ${normalizedEmail} in the Architecture 4 app user store with password "${DEFAULT_PASSWORD}".` }, 201)
  }

  const kc = EXTERNAL_KC[gw.arch]
  const result = await createKeycloakUser(kc, normalizedEmail, first, last).catch((err) => {
    console.error('[ADMIN] Keycloak call failed:', err)
    return { status: 502, error: 'Could not reach Keycloak' }
  })
  if (result.error) return c.json({ error: result.error }, result.status as 400)
  return c.json({ ok: true, message: `Created ${normalizedEmail} in ${kc.label} with password "${DEFAULT_PASSWORD}" and role partner.` }, 201)
})
