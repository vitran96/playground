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

export interface ResetTokenRecord {
  email: string
  expiresAt: number
}

// In-memory local user store for Architecture 4 external side
export const localUsers = new Map<string, LocalUser>()
export const resetTokens = new Map<string, ResetTokenRecord>()

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

export async function createLocalUser(email: string, password: string, displayName?: string): Promise<{ success: boolean; error?: string }> {
  const normalizedEmail = email.trim().toLowerCase()

  // Input validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(normalizedEmail)) {
    return { success: false, error: 'Invalid email address format' }
  }

  // Refuse internal corporate email domain on external sign-up
  if (normalizedEmail.endsWith('@internal.email.com')) {
    return {
      success: false,
      error: 'Company employees must use the internal corporate portal (http://int.localhost:8401)',
    }
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

export function createResetToken(email: string): string | null {
  const normalizedEmail = email.trim().toLowerCase()
  if (!localUsers.has(normalizedEmail)) return null

  const token = crypto.randomBytes(32).toString('hex')
  const expiresAt = Date.now() + 15 * 60 * 1000 // 15 minutes validity
  resetTokens.set(token, { email: normalizedEmail, expiresAt })
  return token
}

export async function consumeResetToken(token: string, newPassword: string): Promise<{ success: boolean; error?: string }> {
  const record = resetTokens.get(token)
  if (!record || Date.now() > record.expiresAt) {
    resetTokens.delete(token)
    return { success: false, error: 'Invalid or expired password reset token' }
  }

  if (newPassword.length < 8) {
    return { success: false, error: 'Password must be at least 8 characters long' }
  }

  const user = localUsers.get(record.email)
  if (!user) {
    resetTokens.delete(token)
    return { success: false, error: 'User account not found' }
  }

  const salt = crypto.randomBytes(16).toString('hex')
  const hash = await hashPassword(newPassword, salt)
  user.salt = salt
  user.hash = hash
  resetTokens.delete(token)

  return { success: true }
}
