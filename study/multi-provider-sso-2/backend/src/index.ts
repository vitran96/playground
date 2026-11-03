/**
 * Backend entrypoint and server initialization.
 * Used by: Architectures 1, 2, 3, 4.
 *
 * Exposes internal API on port 3000. Not exposed directly to host; accessible only via gateways.
 */

import { serve } from '@hono/node-server'
import { Hono } from 'hono'
import { requireGateway, type GatewayVariables } from './gateway.js'
import { sessionRoutes } from './routes/session.js'
import { ssoRoutes } from './routes/sso.js'
import { localAuthRoutes } from './routes/local-auth.js'
import { adminRoutes } from './routes/admin.js'
import { selfServiceRoutes } from './routes/self-service.js'

const app = new Hono<{ Variables: GatewayVariables }>()

// Healthcheck endpoint for container orchestration
app.get('/api/health', (c) => c.json({ ok: true, timestamp: Date.now() }))

// Enforce gateway verification on all architecture routes
app.use('/api/:arch/*', requireGateway)

// Mount feature routes
app.route('/', sessionRoutes)
app.route('/', ssoRoutes)
app.route('/', localAuthRoutes)
app.route('/', adminRoutes)
app.route('/', selfServiceRoutes)

const port = 3000
console.log(`Backend server starting on port ${port}...`)
serve({
  fetch: app.fetch,
  port,
})
