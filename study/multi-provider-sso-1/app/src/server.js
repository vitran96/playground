import path from 'node:path';
import express from 'express';
import session from 'express-session';
import { config } from './config.js';
import { arch1Router } from './arch1.js';
import { arch2Router } from './arch2.js';

const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: false }));

app.use(
  session({
    name: 'demo.sid',
    secret: config.sessionSecret,
    resave: false,
    saveUninitialized: false,
    cookie: {
      httpOnly: true,
      sameSite: 'lax',
      secure: false
    }
  })
);

app.use(express.static(path.resolve('public')));
app.use(arch1Router);
app.use(arch2Router);

app.get(['/', '/architecture-1', '/architecture-2'], (req, res) => {
  res.sendFile(path.resolve('public/index.html'));
});

// Error handling middleware
app.use((err, req, res, next) => {
  const status = err.status || 500;
  if (req.path.startsWith('/api/')) {
    return res.status(status).json({ error: err.message });
  }
  res.status(status).send(`<!doctype html>
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
</html>`);
});

app.listen(config.port, () => {
  console.log(`Server listening on port ${config.port} (${config.appBaseUrl})`);
});
