import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { Hono } from 'hono';
import { serve } from '@hono/node-server';
import { getCookie, setCookie } from 'hono/cookie';
import { config } from './config.js';
import { arch1Router } from './arch1.js';
import { arch2Router } from './arch2.js';
import { archExtraRouter } from './arch_extra.js';

const app = new Hono();
const sessions = new Map();

// Session middleware
app.use('*', async (c, next) => {
  let sid = getCookie(c, 'demo.sid');
  if (!sid || !sessions.has(sid)) {
    sid = crypto.randomUUID();
    sessions.set(sid, {});
    setCookie(c, 'demo.sid', sid, {
      httpOnly: true,
      sameSite: 'Lax',
      path: '/',
      secure: false
    });
  }
  c.set('session', sessions.get(sid));
  await next();
});

// Auth & API routers
app.route('/', arch1Router);
app.route('/', arch2Router);
app.route('/', archExtraRouter);

// Frontend assets & routes served directly by Hono
const indexHtml = fs.readFileSync(path.resolve('public/index.html'), 'utf-8');
const appJs = fs.readFileSync(path.resolve('public/app.js'), 'utf-8');

app.get('/app.js', (c) => {
  c.header('Content-Type', 'text/javascript; charset=utf-8');
  return c.body(appJs);
});

for (const route of [
  '/',
  '/architecture-1',
  '/architecture-2',
  '/architecture-3',
  '/architecture-4',
  '/architecture-5',
  '/architecture-5/internal',
  '/architecture-6',
  '/architecture-7'
]) {
  app.get(route, (c) => c.html(indexHtml));
}

// Error handler
app.onError((err, c) => {
  const status = err.status || 500;
  if (c.req.path.startsWith('/api/')) {
    return c.json({ error: err.message }, status);
  }
  return c.html(`<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Error ${status}</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css">
</head>
<body class="container" style="padding-top: 3rem;">
  <article>
    <header><h2>Authentication Error (${status})</h2></header>
    <p>${err.message}</p>
    <footer><a role="button" href="/">Back to Home</a></footer>
  </article>
</body>
</html>`, status);
});

serve({ fetch: app.fetch, port: config.port }, () => {
  console.log(`Server listening on port ${config.port} (${config.appBaseUrl})`);
});
