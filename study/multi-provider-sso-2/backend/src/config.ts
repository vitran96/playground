/**
 * Gateway registry and JWT configuration.
 * Used by: Architectures 1, 2, 3, 4.
 *
 * Single source of truth for gateway identity, zones, cookie names, and allowed trust parameters.
 */

export type Zone = 'any' | 'internal' | 'external'
export type LoginUi = 'redirect' | 'choice' | 'form'

export interface Gateway {
  id: string
  arch: 1 | 2 | 3 | 4
  zone: Zone
  loginUi: LoginUi
  oidc: boolean // false = no SSO in front (gw-4-ext)
  cookieName: string
  secret: string
}

function required(name: string): string {
  const v = process.env[name]
  if (!v) {
    throw new Error(`Missing required environment variable: ${name}`)
  }
  return v
}

const g = (
  id: string,
  arch: Gateway['arch'],
  zone: Zone,
  loginUi: LoginUi,
  oidc = true
): Gateway => ({
  id,
  arch,
  zone,
  loginUi,
  oidc,
  // Distinct cookie names isolate session jars across architectures and internal/external zones
  cookieName: zone === 'any' ? `architecture-${arch}-session` : `architecture-${arch}-${zone}-session`,
  // Per-gateway secret prevents forged gateway headers
  secret: required(`GW_SECRET_${id.toUpperCase().replaceAll('-', '_')}`),
})

export const GATEWAYS: Record<string, Gateway> = Object.fromEntries([
  g('gw-1', 1, 'any', 'redirect'),
  g('gw-2', 2, 'any', 'choice'),
  g('gw-3-int', 3, 'internal', 'redirect'),
  g('gw-3-ext', 3, 'external', 'redirect'),
  g('gw-4-int', 4, 'internal', 'redirect'),
  g('gw-4-ext', 4, 'external', 'form', false),
].map((x) => [x.id, x]))

// Token audience separates entry points so tokens cannot be replayed across zones or architectures
export const audienceOf = (gw: Gateway): string => `architecture-${gw.arch}:${gw.zone}`

export const JWT = {
  issuer: 'demo-backend',
  secret: required('JWT_SECRET'),
  ttlSeconds: 15 * 60, // 15 minutes lifetime, no refresh
}
