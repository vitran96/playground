/**
 * Mock Product API for the Angular 17 practice project.
 *
 *   npm install
 *   npm start                 -> http://localhost:3000
 *   DELAY=800 npm start       -> add latency (ms) to every response
 *   CHAOS=0.2 npm start       -> 20% of requests fail with 500 (test error handling)
 *   KEYCLOAK_ISSUER=http://localhost:8080/realms/shop npm start
 *                             -> validate Keycloak JWTs; DELETE needs realm role ADMIN
 *
 * Error format mimics Spring Boot's ProblemDetail + field errors,
 * so the Angular code you write here transfers to a Java backend.
 */
require('dotenv').config();

const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;
const DELAY = Number(process.env.DELAY || 300);
const CHAOS = Number(process.env.CHAOS || 0);

app.use(cors());
app.use(express.json());

// ---------- middleware: latency, chaos, fake auth ----------
app.use((req, res, next) => setTimeout(next, DELAY));

app.use((req, res, next) => {
  const forceFail = req.query.fail === '1';
  if (forceFail || (CHAOS > 0 && Math.random() < CHAOS)) {
    return problem(res, 500, 'Internal Server Error', 'Simulated server failure');
  }
  next();
});

// Any request with "Authorization: Bearer expired" gets 401 (test your interceptor)
app.use((req, res, next) => {
  if (req.headers.authorization === 'Bearer expired') {
    return problem(res, 401, 'Unauthorized', 'Token expired');
  }
  next();
});

// ---------- optional: real Keycloak JWT validation ----------
// KEYCLOAK_ISSUER=http://localhost:8080/realms/shop npm start
// Every /api call (except /api/health) then needs a valid Keycloak access token,
// and deletes need the realm role ADMIN — the same checks a Spring resource server makes.
const ISSUER = process.env.KEYCLOAK_ISSUER;
if (ISSUER) {
  const verifierPromise = import('jose').then(({ createRemoteJWKSet, jwtVerify }) => {
    const jwks = createRemoteJWKSet(new URL(`${ISSUER}/protocol/openid-connect/certs`));
    return token => jwtVerify(token, jwks, { issuer: ISSUER });
  });

  app.use('/api', async (req, res, next) => {
    if (req.path === '/health') return next();

    const header = req.headers.authorization || '';
    if (!header.startsWith('Bearer ')) {
      return problem(res, 401, 'Unauthorized', 'Missing bearer token');
    }

    let payload;
    try {
      const verify = await verifierPromise;
      ({ payload } = await verify(header.slice('Bearer '.length)));
    } catch (e) {
      return problem(res, 401, 'Unauthorized', `Invalid token: ${e.code || e.message}`);
    }
    const roles = (payload.realm_access && payload.realm_access.roles) || [];
    req.user = { username: payload.preferred_username, roles };
    const isDelete = req.method === 'DELETE' || req.path.endsWith('/bulk-delete');
    if (isDelete && !roles.includes('ADMIN')) {
      return problem(res, 403, 'Forbidden', 'Role ADMIN required');
    }
    next();
  });
}

// ---------- seed data ----------
const CATEGORIES = [
  { id: 1, name: 'Laptops' },
  { id: 2, name: 'Phones' },
  { id: 3, name: 'Accessories' },
  { id: 4, name: 'Monitors' },
];
const STATUSES = ['ACTIVE', 'DRAFT', 'ARCHIVED'];

let nextId = 1;
let products = Array.from({ length: 87 }, (_, i) => {
  const cat = CATEGORIES[i % CATEGORIES.length];
  return {
    id: nextId++,
    name: `${cat.name.slice(0, -1)} ${String.fromCharCode(65 + (i % 26))}${i + 1}`,
    sku: `SKU-${String(1000 + i)}`,
    categoryId: cat.id,
    price: Math.round((50 + ((i * 37) % 1950)) * 100) / 100,
    stock: (i * 13) % 120,
    status: STATUSES[i % STATUSES.length],
    contactEmail: `owner${i + 1}@shop.test`,
    tags: i % 2 ? ['sale'] : ['new'],
    createdAt: new Date(Date.UTC(2025, 0, 1 + i)).toISOString(),
  };
});

const takenEmails = new Set(['taken@shop.test', 'admin@shop.test']);

// ---------- helpers ----------
function problem(res, status, title, detail, errors) {
  return res.status(status).json({
    type: 'about:blank',
    title,
    status,
    detail,
    timestamp: new Date().toISOString(),
    ...(errors ? { errors } : {}),
  });
}

function validateProduct(body, currentId) {
  const errors = [];
  const add = (field, message) => errors.push({ field, message });

  if (!body.name || String(body.name).trim().length < 3) add('name', 'Name must be at least 3 characters');
  if (body.name && String(body.name).length > 60) add('name', 'Name must be at most 60 characters');

  if (!/^SKU-\d{4,}$/.test(body.sku || '')) add('sku', 'SKU must look like SKU-1234');
  else if (products.some(p => p.sku === body.sku && p.id !== currentId)) add('sku', 'SKU already exists');

  if (!CATEGORIES.some(c => c.id === Number(body.categoryId))) add('categoryId', 'Unknown category');
  if (typeof body.price !== 'number' || body.price <= 0) add('price', 'Price must be a positive number');
  if (!Number.isInteger(body.stock) || body.stock < 0) add('stock', 'Stock must be an integer >= 0');
  if (!STATUSES.includes(body.status)) add('status', `Status must be one of ${STATUSES.join(', ')}`);
  if (body.contactEmail && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(body.contactEmail)) add('contactEmail', 'Invalid email');

  // Server-only rule (shows why you must map server errors to the form)
  if (body.status === 'ACTIVE' && body.stock === 0) add('stock', 'Active products must have stock > 0');
  return errors;
}

function pick(body) {
  const { name, sku, categoryId, price, stock, status, contactEmail, tags } = body;
  return {
    name, sku, categoryId: Number(categoryId), price, stock, status,
    contactEmail: contactEmail || null, tags: Array.isArray(tags) ? tags : [],
  };
}

// ---------- routes ----------
app.get('/api/health', (req, res) => res.json({ ok: true }));

app.get('/api/categories', (req, res) => res.json(CATEGORIES));

// Who am I? (filled only in Keycloak mode)
app.get('/api/me', (req, res) => res.json(req.user || { username: 'anonymous', roles: [], keycloak: false }));

/**
 * GET /api/products?page=1&size=10&sort=price&order=desc&q=lap&categoryId=1&status=ACTIVE
 * -> { items, total, page, size }
 */
app.get('/api/products', (req, res) => {
  const page = Math.max(1, Number(req.query.page) || 1);
  const size = Math.min(100, Math.max(1, Number(req.query.size) || 10));
  const { sort, order = 'asc', q, categoryId, status } = req.query;

  let list = [...products];
  if (q) {
    const term = String(q).toLowerCase();
    list = list.filter(p => p.name.toLowerCase().includes(term) || p.sku.toLowerCase().includes(term));
  }
  if (categoryId) {
    const ids = String(categoryId).split(',').map(Number);
    list = list.filter(p => ids.includes(p.categoryId));
  }
  if (status) {
    const sts = String(status).split(',');
    list = list.filter(p => sts.includes(p.status));
  }
  if (sort && list.length && sort in list[0]) {
    const dir = order === 'desc' ? -1 : 1;
    list.sort((a, b) => (a[sort] > b[sort] ? dir : a[sort] < b[sort] ? -dir : 0));
  }

  const total = list.length;
  const items = list.slice((page - 1) * size, page * size);
  res.json({ items, total, page, size });
});

app.get('/api/products/:id', (req, res) => {
  const p = products.find(x => x.id === Number(req.params.id));
  if (!p) return problem(res, 404, 'Not Found', `Product ${req.params.id} not found`);
  res.json(p);
});

app.post('/api/products', (req, res) => {
  const errors = validateProduct(req.body);
  if (errors.length) return problem(res, 400, 'Bad Request', 'Validation failed', errors);
  const created = { id: nextId++, ...pick(req.body), createdAt: new Date().toISOString() };
  products.unshift(created);
  res.status(201).json(created);
});

app.put('/api/products/:id', (req, res) => {
  const id = Number(req.params.id);
  const idx = products.findIndex(x => x.id === id);
  if (idx === -1) return problem(res, 404, 'Not Found', `Product ${id} not found`);
  const errors = validateProduct(req.body, id);
  if (errors.length) return problem(res, 400, 'Bad Request', 'Validation failed', errors);
  products[idx] = { ...products[idx], ...pick(req.body) };
  res.json(products[idx]);
});

app.delete('/api/products/:id', (req, res) => {
  const id = Number(req.params.id);
  const before = products.length;
  products = products.filter(p => p.id !== id);
  if (products.length === before) return problem(res, 404, 'Not Found', `Product ${id} not found`);
  res.status(204).end();
});

// Bulk delete — practice table row selection
app.post('/api/products/bulk-delete', (req, res) => {
  const ids = Array.isArray(req.body.ids) ? req.body.ids.map(Number) : [];
  products = products.filter(p => !ids.includes(p.id));
  res.json({ deleted: ids.length });
});

// Async validator endpoint: GET /api/validation/email?value=taken@shop.test -> { available: false }
app.get('/api/validation/email', (req, res) => {
  const value = String(req.query.value || '').toLowerCase();
  res.json({ available: !takenEmails.has(value) });
});

// Async validator endpoint: GET /api/validation/sku?value=SKU-1000&excludeId=5
app.get('/api/validation/sku', (req, res) => {
  const value = String(req.query.value || '');
  const excludeId = Number(req.query.excludeId) || 0;
  res.json({ available: !products.some(p => p.sku === value && p.id !== excludeId) });
});

app.use((req, res) => problem(res, 404, 'Not Found', `No route ${req.method} ${req.path}`));

app.listen(PORT, () => {
  console.log(`Mock API on http://localhost:${PORT}  (delay ${DELAY}ms, chaos ${CHAOS}, keycloak ${ISSUER || 'off'})`);
});
