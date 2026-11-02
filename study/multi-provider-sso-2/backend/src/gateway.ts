/**
 * Gateway trust boundary middleware.
 * Used by: Architectures 1, 2, 3, 4.
 *
 * Verifies that incoming requests originated from an authentic Apache gateway instance
 * by checking shared per-gateway secrets with timing-safe comparison.
 */

import { createMiddleware } from 'hono/factory'
import crypto from 'node:crypto'
import { GATEWAYS, type Gateway } from './config.js'

function safeEqual(a: string | undefined, b: string): boolean {
  if (typeof a !== 'string') return false
  const bufA = Buffer.from(a, 'utf8')
  const bufB = Buffer.from(b, 'utf8')
  // timingSafeEqual requires equal length buffers to avoid timing leaks
  if (bufA.length !== bufB.length) return false
  return crypto.timingSafeEqual(bufA, bufB)
}

export type GatewayVariables = {
  gw: Gateway
}

export const requireGateway = createMiddleware<{ Variables: GatewayVariables }>(async (c, next) => {
  const gatewayId = c.req.header('X-Gateway-Id') ?? ''
  const gatewaySecret = c.req.header('X-Gateway-Secret')
  const gw = GATEWAYS[gatewayId]

  // Reject requests without valid gateway credentials
  if (!gw || !safeEqual(gatewaySecret, gw.secret)) {
    return c.json({ error: 'request did not come through a known gateway' }, 403)
  }

  // Gateway isolation: gateway can only invoke its own architecture API routes
  const archParam = c.req.param('arch')
  if (archParam && archParam !== `architecture-${gw.arch}`) {
    return c.json({ error: `gateway ${gw.id} only serves architecture-${gw.arch}` }, 403)
  }

  c.set('gw', gw)
  await next()
})
