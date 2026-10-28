// Architecture 1: one provider; the role claim is the contract.
export function classifyA1(claims) {
  const roles = Array.isArray(claims.roles) ? claims.roles : [];
  const isInternal = roles.includes('internal_user');
  const isExternal = roles.includes('external_resolver');
  if (isInternal === isExternal) {
    return { userType: null, decidedBy: 'roles claim', reason: `roles=${JSON.stringify(roles)}: need exactly one app role` }; // fail closed
  }
  return {
    userType: isExternal ? 'external' : 'internal',
    decidedBy: 'roles claim',
    reason: `roles contains ${isExternal ? 'external_resolver' : 'internal_user'}`,
  };
}

// Architecture 2: two providers; the provider that completed the flow is the contract.
// providerSide comes from the callback route; iss was already verified against that provider.
export function classifyA2(providerSide, claims) {
  return {
    userType: providerSide, // 'internal' | 'external'
    decidedBy: 'login route + issuer',
    reason: `completed /auth/a2/${providerSide}/callback; iss=${claims.iss}`,
  };
}
