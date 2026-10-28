const users = new Map();

export function upsertUser({ iss, sub, username, name, email, userType }) {
  const key = `${iss}|${sub}`;
  const now = new Date().toISOString();
  const existing = users.get(key);
  const user = {
    key,
    id: key,
    iss,
    sub,
    username: username || sub,
    name: name || username || sub,
    email: email || '',
    userType,
    firstSeen: existing ? existing.firstSeen : now,
    lastSeen: now
  };
  users.set(key, user);
  return user;
}

export function getUser(key) {
  return users.get(key);
}
