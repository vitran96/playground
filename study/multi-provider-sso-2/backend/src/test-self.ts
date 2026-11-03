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
process.env.GW_SECRET_GW_6_INT = 's6int'
process.env.GW_SECRET_GW_6_EXT = 's6ext'
process.env.GW_SECRET_GW_7_INT = 's7int'
process.env.GW_SECRET_GW_7_EXT = 's7ext'

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

// 4. OTP reset: wrong code counts a try, right code resets, code is single-use, resend cooldown
const { createResetCode, consumeResetCode, resetCodes } = await import('./users.js')
assert.strictEqual(createResetCode('nobody@external.com'), null)
const code = createResetCode('TestUser@external.com ')!
assert.match(code, /^\d{6}$/)
assert.strictEqual(createResetCode('testuser@external.com'), null, 'resend within cooldown is ignored')
const wrongCode = code === '000000' ? '000001' : '000000'
assert.strictEqual((await consumeResetCode('testuser@external.com', wrongCode, 'new-password-1')).success, false)
assert.strictEqual(resetCodes.get('testuser@external.com')!.tries, 1)
assert.strictEqual((await consumeResetCode('testuser@external.com', code, 'new-password-1')).success, true)
assert.strictEqual(await verifyPassword('new-password-1', localUsers.get('testuser@external.com')!), true)
assert.strictEqual((await consumeResetCode('testuser@external.com', code, 'new-password-2')).success, false)
const code2 = (resetCodes.clear(), createResetCode('testuser@external.com'))!
for (let i = 0; i < 5; i++) await consumeResetCode('testuser@external.com', code2 === '999999' ? '999998' : '999999', 'x-password-1')
assert.strictEqual((await consumeResetCode('testuser@external.com', code2, 'x-password-1')).success, false, 'locked after 5 tries')

// Arch 7: Keycloak-backed accounts use a prefixed key, so an app-local code never matches them
const { checkResetCode } = await import('./users.js')
const kcCode = createResetCode('kc2:carol@partner.example.com', true)!
assert.strictEqual(checkResetCode('carol@partner.example.com', kcCode), false)
assert.strictEqual(checkResetCode('kc2:carol@partner.example.com', kcCode), true)
assert.strictEqual(checkResetCode('kc2:carol@partner.example.com', kcCode), false, 'single-use')
console.log('✓ OTP reset code flow verified')

console.log('All backend checks passed!')
