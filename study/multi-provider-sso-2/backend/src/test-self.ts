/**
 * Self-test verification suite for core backend auth logic.
 * Run with: npx tsx src/test-self.ts
 */

import assert from 'node:assert'

process.env.JWT_SECRET = 'dev-test-secret-at-least-32-bytes-long-1234567890'
process.env.COOKIE_SECURE = 'false'
process.env.GW_SECRET_GW_1 = 's1'
process.env.GW_SECRET_GW_2 = 's2'
process.env.GW_SECRET_GW_3_INT = 's3int'
process.env.GW_SECRET_GW_3_EXT = 's3ext'
process.env.GW_SECRET_GW_4_INT = 's4int'
process.env.GW_SECRET_GW_4_EXT = 's4ext'

const { GATEWAYS, audienceOf } = await import('./config.js')
const { createLocalUser, verifyPassword, localUsers } = await import('./users.js')
const { sign, verify } = await import('hono/jwt')

console.log('Running backend self-checks...')

// 1. Audience isolation test
const tokenInt = await sign(
  {
    iss: 'demo-backend',
    aud: audienceOf(GATEWAYS['gw-3-int']),
    sub: 'alice',
  },
  process.env.JWT_SECRET,
  'HS256'
)

const claims = await verify(tokenInt, process.env.JWT_SECRET, 'HS256')
assert.strictEqual(claims.aud, 'architecture-3:internal')
assert.notStrictEqual(claims.aud, audienceOf(GATEWAYS['gw-3-ext']))
console.log('✓ Audience isolation verified')

// 2. Refusal of internal email domain on external sign-up
const signupInternal = await createLocalUser('someone@internal.email.com', 'password123')
assert.strictEqual(signupInternal.success, false)
console.log('✓ Internal email domain blocked on external signup')

// 3. Local user creation and password check
const signupValid = await createLocalUser('testuser@external.com', 'valid-password-123', 'Test User')
assert.strictEqual(signupValid.success, true)
const u = localUsers.get('testuser@external.com')!
assert.ok(u)
const ok = await verifyPassword('valid-password-123', u)
assert.strictEqual(ok, true)
const bad = await verifyPassword('wrong-password', u)
assert.strictEqual(bad, false)
console.log('✓ Local user password hashing & verification verified')

console.log('All backend checks passed!')
