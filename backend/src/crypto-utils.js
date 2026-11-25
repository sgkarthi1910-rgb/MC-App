const crypto = require('node:crypto');

const SCRYPT_PARAMS = {
  N: 32768, // CPU/memory cost
  r: 8,     // Block size
  p: 1,     // Parallelization
  maxmem: 64 * 1024 * 1024
};

const KEY_LEN = 64;

/**
 * Hashes a plaintext password using Scrypt (RFC 7914 memory-hard algorithm,
 * cryptographically equivalent security to Argon2id).
 *
 * @param {string} password
 * @returns {Promise<string>} serialized hash string
 */
function hashPassword(password) {
  return new Promise((resolve, reject) => {
    const salt = crypto.randomBytes(32).toString('hex');
    crypto.scrypt(password, salt, KEY_LEN, SCRYPT_PARAMS, (err, derivedKey) => {
      if (err) return reject(err);
      resolve(`$scrypt$N=${SCRYPT_PARAMS.N},r=${SCRYPT_PARAMS.r},p=${SCRYPT_PARAMS.p}$${salt}$${derivedKey.toString('hex')}`);
    });
  });
}

/**
 * Synchronous hash version for initialization seeding.
 */
function hashPasswordSync(password) {
  const salt = crypto.randomBytes(32).toString('hex');
  const derivedKey = crypto.scryptSync(password, salt, KEY_LEN, SCRYPT_PARAMS);
  return `$scrypt$N=${SCRYPT_PARAMS.N},r=${SCRYPT_PARAMS.r},p=${SCRYPT_PARAMS.p}$${salt}$${derivedKey.toString('hex')}`;
}

/**
 * Verifies a plaintext password against a stored serialized hash.
 * Uses constant-time comparison to protect against timing attacks.
 *
 * @param {string} password
 * @param {string} storedHash
 * @returns {Promise<boolean>}
 */
function verifyPassword(password, storedHash) {
  return new Promise((resolve) => {
    if (!storedHash || typeof storedHash !== 'string') {
      return resolve(false);
    }
    const parts = storedHash.split('$');
    if (parts.length < 5 || parts[1] !== 'scrypt') {
      return resolve(false);
    }

    const salt = parts[3];
    const expectedKeyHex = parts[4];
    const expectedBuffer = Buffer.from(expectedKeyHex, 'hex');

    crypto.scrypt(password, salt, expectedBuffer.length, SCRYPT_PARAMS, (err, derivedKey) => {
      if (err) return resolve(false);
      try {
        const isMatch = crypto.timingSafeEqual(derivedKey, expectedBuffer);
        resolve(isMatch);
      } catch {
        resolve(false);
      }
    });
  });
}

/**
 * Generates a cryptographically strong session token (64 hex characters)
 * and its SHA-256 hash for database storage.
 */
function generateSessionToken() {
  const rawToken = crypto.randomBytes(32).toString('hex');
  const tokenHash = hashToken(rawToken);
  return { rawToken, tokenHash };
}

function hashToken(rawToken) {
  return crypto.createHash('sha256').update(rawToken).digest('hex');
}

function generateId(prefix = 'id') {
  return `${prefix}_${crypto.randomBytes(8).toString('hex')}`;
}

module.exports = {
  hashPassword,
  hashPasswordSync,
  verifyPassword,
  generateSessionToken,
  hashToken,
  generateId
};
