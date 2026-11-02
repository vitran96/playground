/**
 * App session management (JWT minting and verification, cookie helpers).
 * Used by: Architectures 1, 2, 3, 4.
 *
 * Mints short-lived HS256 JWTs stored in HttpOnly cookies.
 * Production note: Production should use separate signing keys per zone so internal keys
 * never exist on the external path; the demo uses a single key plus audience validation.
 */

import type { Context } from 'hono'
import { getCookie, setCookie, deleteCookie } from 'hono/cookie'
import { sign, verify } from 'hono/jwt'
import { JWT, audienceOf, type Gateway } from './config.js'

export interface SessionUser {
  username: string
  displayName: string
  roles: string[]
  userType: 'internal' | 'external'
}

export interface AppJwtPayload {
  iss: string
  aud: string
  gw: string
  sub: string
  name: string
  roles: string[]
  user_type: 'internal' | 'external'
  iat: number
  exp: number
  [key: string]: unknown
}

export async function startSession(c: Context, gw: Gateway, u: SessionUser): Promise<string> {
  const now = Math.floor(Date.now() / 1000)
  // Mint application-specific JWT containing audience scoped to entry gateway
  const token = await sign(
    {
      iss: JWT.issuer,
      aud: audienceOf(gw),
      gw: gw.id,
      sub: u.username,
      name: u.displayName,
      roles: u.roles,
      user_type: u.userType,
      iat: now,
      exp: now + JWT.ttlSeconds,
    },
    JWT.secret,
    'HS256'
  )

  // Set secure session cookie: HttpOnly prevents XSS theft, SameSite=Lax mitigates CSRF
  setCookie(c, gw.cookieName, token, {
    httpOnly: true,
    sameSite: 'Lax',
    path: '/',
    maxAge: JWT.ttlSeconds,
    secure: process.env.COOKIE_SECURE === 'true',
  })

  return token
}

export async function readSession(c: Context, gw: Gateway): Promise<AppJwtPayload | null> {
  const token = getCookie(c, gw.cookieName)
  if (!token) return null

  try {
    const claims = (await verify(token, JWT.secret, 'HS256')) as unknown as AppJwtPayload
    // Crucial audience check: token minted on internal entry point is rejected on external (and vice versa)
    if (claims.iss !== JWT.issuer || claims.aud !== audienceOf(gw)) {
      return null
    }
    return claims
  } catch {
    return null
  }
}

export function clearSession(c: Context, gw: Gateway): void {
  deleteCookie(c, gw.cookieName, {
    path: '/',
    secure: process.env.COOKIE_SECURE === 'true',
  })
}
