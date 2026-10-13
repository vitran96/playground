# ProductAdmin

## Folder structure

src/app/
  core/     interceptors, guards, layout, auth
  shared/   ui (data-table), pipes, directives
  features/
    products/
      pages/ components/ state/ data/
      products.routes.ts
  app.config.ts  app.routes.ts

## Mock Product API

Express API for the Angular 17 practice app. In-memory data (resets on restart).

```bash
npm install
npm start                 # http://localhost:3000
DELAY=1000 npm start      # slow network -> test loading states
CHAOS=0.3 npm start       # 30% random 500s -> test error handling / retry
```

### Endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/api/products?page=1&size=10&sort=price&order=desc&q=lap&categoryId=1,2&status=ACTIVE` | Returns `{ items, total, page, size }` |
| GET | `/api/products/:id` | 404 if missing |
| POST | `/api/products` | 201 / 400 with field errors |
| PUT | `/api/products/:id` | 200 / 400 / 404 |
| DELETE | `/api/products/:id` | 204 / 404 |
| POST | `/api/products/bulk-delete` | body `{ ids: number[] }` |
| GET | `/api/categories` | for select dropdowns |
| GET | `/api/me` | current user and roles (Keycloak mode) |
| GET | `/api/validation/email?value=x` | `{ available }` — `taken@shop.test` is taken |
| GET | `/api/validation/sku?value=SKU-1000&excludeId=5` | `{ available }` |

Add `?fail=1` to any request to force a 500. Send `Authorization: Bearer expired` to get a 401.

### Optional: real Keycloak tokens (Exercise 6)

```bash
docker run -p 8080:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.0 start-dev

KEYCLOAK_ISSUER=http://localhost:8080/realms/shop npm start
```

In this mode every `/api/*` call (except `/api/health`) needs a valid Keycloak access token. The API checks the signature against Keycloak's JWKS keys and checks the issuer. `DELETE` and bulk delete need the realm role `ADMIN`. You get 401 without a valid token and 403 without the role. `GET /api/me` returns your username and roles.

Keycloak setup: create realm `shop`; create a public client `product-admin` with standard flow, PKCE S256, redirect URI `http://localhost:4200/*` and web origin `http://localhost:4200`; create realm roles `ADMIN` and `USER`; create users `alice` (ADMIN) and `bob` (USER).

### Product shape

```ts
interface Product {
  id: number; name: string; sku: string; categoryId: number;
  price: number; stock: number; status: 'ACTIVE' | 'DRAFT' | 'ARCHIVED';
  contactEmail: string | null; tags: string[]; createdAt: string;
}
```

### Error shape (Spring Boot ProblemDetail style)

```json
{
  "title": "Bad Request", "status": 400, "detail": "Validation failed",
  "errors": [{ "field": "sku", "message": "SKU already exists" }]
}
```

Server-only rule to practise mapping server errors to the form: an `ACTIVE` product with `stock: 0` is rejected.

### Angular dev proxy (avoids CORS, mirrors production)

`proxy.conf.json` in the Angular project root:

```json
{ "/api": { "target": "http://localhost:3000", "secure": false } }
```

`angular.json` → `projects.<app>.architect.serve.options.proxyConfig: "proxy.conf.json"`, then call `/api/...` with relative URLs.

## Keycloak for local practice

```bash
docker compose up -d          # Keycloak on http://localhost:8080
docker compose logs -f        # wait for "Realm 'shop' imported" / "Running the server"
docker compose down           # stop; the next "up" re-imports the realm fresh
```

Admin console: http://localhost:8080/admin. Log in as `admin` / `admin`.

### What the realm import creates (realm `shop`)

| User | Password | Realm roles | Use it to test |
|---|---|---|---|
| alice | alice123 | ADMIN, USER | Everything works, including delete |
| bob | bob123 | USER | Delete is hidden in the UI and the API returns 403 |
| carol | carol123 | none | Logged in but no roles; your role guard should send her to /forbidden |
| dave | dave123 | USER (disabled) | Login is refused because the account is disabled |

| Client | Type | Notes |
|---|---|---|
| `product-admin` | public, PKCE S256 | For the Angular app. Redirect URI is `http://localhost:4200/*` and the web origin is `http://localhost:4200`. Direct access grants are on, so you can get tokens with curl. Tokens include `aud: product-api`. |
| `product-service` | confidential | Secret is `product-service-secret`. Service account has the USER role. Use it to practise the client-credentials grant. |

Access tokens live for 5 minutes, so you can watch refresh happen.

### Try it without Angular

```bash
# user token (password grant, for testing only; the SPA uses Authorization Code + PKCE)
TOKEN=$(curl -s http://localhost:8080/realms/shop/protocol/openid-connect/token \
  -d grant_type=password -d client_id=product-admin \
  -d username=alice -d password=alice123 | jq -r .access_token)

# service token (client credentials)
curl -s http://localhost:8080/realms/shop/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=product-service \
  -d client_secret=product-service-secret | jq .
```

Paste a token into https://jwt.io to see `iss`, `aud`, `exp` and `realm_access.roles`.

### With the mock API

```bash
cd ../mock-api && npm install
KEYCLOAK_ISSUER=http://localhost:8080/realms/shop npm start

curl -s localhost:3000/api/me -H "Authorization: Bearer $TOKEN"
curl -s -X DELETE localhost:3000/api/products/5 -H "Authorization: Bearer $TOKEN" -w "%{http_code}\n"
```

The delete returns 204 with alice's token and 403 with bob's. On Windows PowerShell, set the variable with `$env:KEYCLOAK_ISSUER="http://localhost:8080/realms/shop"; npm start`.

### With Spring Boot

```yaml
spring.security.oauth2.resourceserver.jwt.issuer-uri: http://localhost:8080/realms/shop
```

### Notes

- **Dev only.** Passwords are plain text in `import/shop-realm.json`, and Keycloak hashes them on import. Never do this for a real environment.
- **Re-importing.** The import runs only when the realm doesn't exist yet. Data lives inside the container, so `docker compose down` followed by `up` gives you a fresh realm. To change users permanently, edit the JSON and recreate the container.
- **Older Keycloak images.** These use `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` instead of `KC_BOOTSTRAP_ADMIN_*`.
- **"Invalid issuer" errors.** Keep using `localhost:8080` everywhere. If an API calls Keycloak by another hostname (for example from inside Docker), the `iss` claim won't match.
