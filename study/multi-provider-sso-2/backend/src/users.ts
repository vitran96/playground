/**
 * User store and profile enrichment (in-memory).
 * Used by: Architectures 1, 2, 3 (profiles) and Architecture 4 (profiles + local users).
 *
 * Demonstrates app-level profile enrichment for SSO identities and local credential storage
 * with scrypt hashing for Architecture 4's app-owned login.
 */

import crypto from 'node:crypto'

export interface AppProfile {
  displayName: string
  team: string
}

export const APP_PROFILES: Record<string, AppProfile> = {
  alice: { displayName: 'Alice Engineer', team: 'Core Platform' },
  bob: { displayName: 'Bob Developer', team: 'Product Engineering' },
  'carol@partner.example.com': { displayName: 'Carol Partner', team: 'External Integrations' },
  'dave@customer.example.com': { displayName: 'Dave Customer', team: 'Customer Accounts' },
  'erin@partner.example.com': { displayName: 'Erin Local', team: 'Vendor Operations' },
}

export function profileName(username: string): string {
  return APP_PROFILES[username]?.displayName || username
}

interface LocalUser {
  displayName: string
  roles: string[]
  salt: string
  hash: string // hex encoded
}

export interface ResetCodeRecord {
  code: string
  expiresAt: number
  tries: number
}

// In-memory local user store for Architecture 4 external side
export const localUsers = new Map<string, LocalUser>()
// Keyed by normalized email: one active reset code per account
export const resetCodes = new Map<string, ResetCodeRecord>()

function hashPassword(password: string, salt: string): Promise<string> {
  return new Promise((resolve, reject) => {
    crypto.scrypt(password, salt, 64, { N: 16384, r: 8, p: 1 }, (err, derivedKey) => {
      if (err) reject(err)
      else resolve(derivedKey.toString('hex'))
    })
  })
}

// Seed test user erin@partner.example.com / erin at startup
async function initSeedUsers() {
  const salt = crypto.randomBytes(16).toString('hex')
  const hash = await hashPassword('erin', salt)
  localUsers.set('erin@partner.example.com', {
    displayName: 'Erin Local',
    roles: ['partner'],
    salt,
    hash,
  })
}

initSeedUsers().catch((err) => {
  console.error('Failed to initialize seed local users:', err)
})

export async function verifyPassword(password: string, user: LocalUser): Promise<boolean> {
  const computedHash = await hashPassword(password, user.salt)
  const computedBuf = Buffer.from(computedHash, 'hex')
  const expectedBuf = Buffer.from(user.hash, 'hex')
  if (computedBuf.length !== expectedBuf.length) return false
  return crypto.timingSafeEqual(computedBuf, expectedBuf)
}

// Returns an error message, or null if the email is acceptable for an external account
export function externalEmailError(normalizedEmail: string): string | null {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail) || normalizedEmail.length > 254) {
    return 'Invalid email address format'
  }
  if (normalizedEmail.endsWith('@internal.email.com')) {
    return 'Company employees must use the internal corporate portal, not an external account'
  }
  return null
}

export async function createLocalUser(email: string, password: string, displayName?: string): Promise<{ success: boolean; error?: string }> {
  const normalizedEmail = email.trim().toLowerCase()

  const emailError = externalEmailError(normalizedEmail)
  if (emailError) {
    return { success: false, error: emailError }
  }

  if (password.length < 8) {
    return { success: false, error: 'Password must be at least 8 characters long' }
  }

  if (localUsers.has(normalizedEmail)) {
    return { success: false, error: 'An account with this email already exists' }
  }

  const salt = crypto.randomBytes(16).toString('hex')
  const hash = await hashPassword(password, salt)
  localUsers.set(normalizedEmail, {
    displayName: displayName?.trim() || normalizedEmail.split('@')[0],
    roles: ['partner'],
    salt,
    hash,
  })

  return { success: true }
}

const RESET_TTL_MS = 10 * 60 * 1000
const RESET_MAX_TRIES = 5
const RESET_RESEND_COOLDOWN_MS = 60 * 1000

// Returns a fresh 6-digit code, or null if the account is unknown or a code was issued under a minute ago
export function createResetCode(email: string, known?: boolean): string | null {
  const normalizedEmail = email.trim().toLowerCase()
  if (!(known ?? localUsers.has(normalizedEmail))) return null

  const existing = resetCodes.get(normalizedEmail)
  if (existing && existing.expiresAt - RESET_TTL_MS + RESET_RESEND_COOLDOWN_MS > Date.now()) return null

  const code = crypto.randomInt(0, 1_000_000).toString().padStart(6, '0')
  resetCodes.set(normalizedEmail, { code, expiresAt: Date.now() + RESET_TTL_MS, tries: 0 })
  return code
}

// True if the code matches; a match consumes it, a miss counts one of the allowed tries
export function checkResetCode(email: string, code: string): boolean {
  const normalizedEmail = email.trim().toLowerCase()
  const record = resetCodes.get(normalizedEmail)
  if (!record) return false
  if (Date.now() > record.expiresAt || record.tries >= RESET_MAX_TRIES) {
    resetCodes.delete(normalizedEmail)
    return false
  }

  record.tries++
  const expected = Buffer.from(record.code)
  const given = Buffer.from(code.trim())
  if (expected.length !== given.length || !crypto.timingSafeEqual(expected, given)) return false

  resetCodes.delete(normalizedEmail)
  return true
}

export async function consumeResetCode(email: string, code: string, newPassword: string): Promise<{ success: boolean; error?: string }> {
  const normalizedEmail = email.trim().toLowerCase()
  const invalid = { success: false, error: 'Invalid or expired reset code' }

  if (newPassword.length < 8) {
    return { success: false, error: 'Password must be at least 8 characters long' }
  }

  if (!checkResetCode(normalizedEmail, code)) return invalid
  const user = localUsers.get(normalizedEmail)
  if (!user) return invalid

  const salt = crypto.randomBytes(16).toString('hex')
  user.hash = await hashPassword(newPassword, salt)
  user.salt = salt

  return { success: true }
}
