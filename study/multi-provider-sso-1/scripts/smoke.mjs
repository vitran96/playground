// Smoke test for OIDC multi-provider demo
const APP_BASE = process.env.APP_BASE_URL || 'http://localhost:3000';

const checks = [
  // 1. Discovery issuer checks
  async () => {
    const res = await fetch('http://localhost:8080/realms/central/.well-known/openid-configuration');
    if (!res.ok) throw new Error(`Central discovery failed with HTTP ${res.status}`);
    const data = await res.json();
    if (data.issuer !== 'http://localhost:8080/realms/central') {
      throw new Error(`Central discovery issuer mismatch: expected http://localhost:8080/realms/central, got ${data.issuer}`);
    }
    return 'Central realm discovery issuer is http://localhost:8080/realms/central';
  },
  async () => {
    const res = await fetch('http://localhost:8081/realms/internal/.well-known/openid-configuration');
    if (!res.ok) throw new Error(`Internal discovery failed with HTTP ${res.status}`);
    const data = await res.json();
    if (data.issuer !== 'http://localhost:8081/realms/internal') {
      throw new Error(`Internal discovery issuer mismatch: expected http://localhost:8081/realms/internal, got ${data.issuer}`);
    }
    return 'Internal realm discovery issuer is http://localhost:8081/realms/internal';
  },
  async () => {
    const res = await fetch('http://localhost:8082/realms/external/.well-known/openid-configuration');
    if (!res.ok) throw new Error(`External discovery failed with HTTP ${res.status}`);
    const data = await res.json();
    if (data.issuer !== 'http://localhost:8082/realms/external') {
      throw new Error(`External discovery issuer mismatch: expected http://localhost:8082/realms/external, got ${data.issuer}`);
    }
    return 'External realm discovery issuer is http://localhost:8082/realms/external';
  },

  // 2. API 401 checks
  async () => {
    const res = await fetch(`${APP_BASE}/api/a1/me`);
    if (res.status !== 401) throw new Error(`Expected /api/a1/me to return 401, got ${res.status}`);
    return '/api/a1/me unauthenticated returns 401';
  },
  async () => {
    const res = await fetch(`${APP_BASE}/api/a2/me`);
    if (res.status !== 401) throw new Error(`Expected /api/a2/me to return 401, got ${res.status}`);
    return '/api/a2/me unauthenticated returns 401';
  },
  async () => {
    const res = await fetch(`${APP_BASE}/api/a3/me`);
    if (res.status !== 401) throw new Error(`Expected /api/a3/me to return 401, got ${res.status}`);
    return '/api/a3/me unauthenticated returns 401';
  },
  async () => {
    const res = await fetch(`${APP_BASE}/api/a6/me`);
    if (res.status !== 401) throw new Error(`Expected /api/a6/me to return 401, got ${res.status}`);
    return '/api/a6/me unauthenticated returns 401';
  },
  async () => {
    const res = await fetch(`${APP_BASE}/api/a7/me`);
    if (res.status !== 401) throw new Error(`Expected /api/a7/me to return 401, got ${res.status}`);
    return '/api/a7/me unauthenticated returns 401';
  },

  // 3. Login redirect checks (no redirect follow)
  async () => {
    const res = await fetch(`${APP_BASE}/auth/a1/login`, { redirect: 'manual' });
    if (res.status !== 302) throw new Error(`Expected /auth/a1/login to return 302, got ${res.status}`);
    const loc = res.headers.get('location') || '';
    if (!loc.startsWith('http://localhost:8080/realms/central/protocol/openid-connect/auth')) {
      throw new Error(`Invalid redirect Location for A1: ${loc}`);
    }
    if (!loc.includes('code_challenge_method=S256')) {
      throw new Error(`Missing code_challenge_method=S256 in A1 Location: ${loc}`);
    }
    return '/auth/a1/login redirects to central realm with PKCE S256';
  },
  async () => {
    const res = await fetch(`${APP_BASE}/auth/a2/internal/login`, { redirect: 'manual' });
    if (res.status !== 302) throw new Error(`Expected /auth/a2/internal/login to return 302, got ${res.status}`);
    const loc = res.headers.get('location') || '';
    if (!loc.startsWith('http://localhost:8081/realms/internal/protocol/openid-connect/auth')) {
      throw new Error(`Invalid redirect Location for A2 internal: ${loc}`);
    }
    if (!loc.includes('code_challenge_method=S256')) {
      throw new Error(`Missing code_challenge_method=S256 in A2 internal Location: ${loc}`);
    }
    return '/auth/a2/internal/login redirects to internal realm with PKCE S256';
  },
  async () => {
    const res = await fetch(`${APP_BASE}/auth/a2/external/login`, { redirect: 'manual' });
    if (res.status !== 302) throw new Error(`Expected /auth/a2/external/login to return 302, got ${res.status}`);
    const loc = res.headers.get('location') || '';
    if (!loc.startsWith('http://localhost:8082/realms/external/protocol/openid-connect/auth')) {
      throw new Error(`Invalid redirect Location for A2 external: ${loc}`);
    }
    if (!loc.includes('code_challenge_method=S256')) {
      throw new Error(`Missing code_challenge_method=S256 in A2 external Location: ${loc}`);
    }
    return '/auth/a2/external/login redirects to external realm with PKCE S256';
  },
  async () => {
    const res = await fetch(`${APP_BASE}/auth/a6/login`, {
      headers: { 'X-Network-Zone': 'external' },
      redirect: 'manual'
    });
    if (res.status !== 302) throw new Error(`Expected /auth/a6/login to return 302, got ${res.status}`);
    const loc = res.headers.get('location') || '';
    if (!loc.startsWith('http://localhost:8082/realms/external')) {
      throw new Error(`Expected external realm for X-Network-Zone: external, got ${loc}`);
    }
    return '/auth/a6/login with X-Network-Zone: external redirects to external realm';
  },
  async () => {
    const res = await fetch(`${APP_BASE}/auth/a7/login?hint=internal`, { redirect: 'manual' });
    if (res.status !== 302) throw new Error(`Expected /auth/a7/login to return 302, got ${res.status}`);
    const loc = res.headers.get('location') || '';
    if (!loc.includes('kc_idp_hint=internal')) {
      throw new Error(`Missing kc_idp_hint=internal in A7 Location: ${loc}`);
    }
    return '/auth/a7/login?hint=internal redirects to central realm with kc_idp_hint=internal';
  }
];

let failed = 0;
for (const check of checks) {
  try {
    const msg = await check();
    console.log(`PASS: ${msg}`);
  } catch (err) {
    console.error(`FAIL: ${err.message}`);
    failed++;
  }
}

if (failed > 0) {
  console.error(`\n${failed} check(s) failed.`);
  process.exit(1);
} else {
  console.log(`\nAll checks passed.`);
}
