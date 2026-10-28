const appBaseUrl = process.env.APP_BASE_URL || 'http://localhost:3000';
const port = parseInt(process.env.PORT || '3000', 10);
const sessionSecret = process.env.SESSION_SECRET || 'change-me-demo-only';

export const config = {
  port,
  appBaseUrl,
  sessionSecret,
  providers: {
    'a1': {
      key: 'a1',
      issuer: process.env.A1_ISSUER || 'http://localhost:8080/realms/central',
      backchannel: process.env.A1_BACKCHANNEL || 'http://kc-central:8080/realms/central',
      clientId: process.env.A1_CLIENT_ID || 'demo-app-a1',
      clientSecret: process.env.A1_CLIENT_SECRET || 'a1-secret',
      redirectUri: `${appBaseUrl}/auth/a1/callback`
    },
    'a2-internal': {
      key: 'a2-internal',
      issuer: process.env.A2_INT_ISSUER || 'http://localhost:8081/realms/internal',
      backchannel: process.env.A2_INT_BACKCHANNEL || 'http://kc-internal:8080/realms/internal',
      clientId: process.env.A2_INT_CLIENT_ID || 'demo-app-a2',
      clientSecret: process.env.A2_INT_CLIENT_SECRET || 'a2-internal-secret',
      redirectUri: `${appBaseUrl}/auth/a2/internal/callback`
    },
    'a2-external': {
      key: 'a2-external',
      issuer: process.env.A2_EXT_ISSUER || 'http://localhost:8082/realms/external',
      backchannel: process.env.A2_EXT_BACKCHANNEL || 'http://kc-external:8080/realms/external',
      clientId: process.env.A2_EXT_CLIENT_ID || 'demo-app-a2',
      clientSecret: process.env.A2_EXT_CLIENT_SECRET || 'a2-external-secret',
      redirectUri: `${appBaseUrl}/auth/a2/external/callback`
    }
  }
};
