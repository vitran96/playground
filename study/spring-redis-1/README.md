# Spring Boot 4 + Valkey (Redis) Caching & Pub/Sub Demo

Sample project demonstrating Spring Cache with Valkey and Redis pub/sub messaging.

**Stack:** Java 25, Spring Boot 4, Spring Data Redis, Valkey 8

## Prerequisites

- Java 25
- Docker & Docker Compose

## Run

```bash
# Start Valkey
docker compose up valkey -d

# Run the app
gradle bootRun
```

Or run everything in Docker:

```bash
docker compose up --build
```

## Caching Endpoints

| Method | Path | Strategy | Cache |
|--------|------|----------|-------|
| GET | `/api/v1/items/standard` | `@Cacheable` | `items-default` (5 min) |
| GET | `/api/v1/items/conditional?category=X` | `@Cacheable` + SpEL | `items-conditional` (5 min) |
| GET | `/api/v1/items/short-lived` | `@Cacheable` | `items-short-ttl` (60s) |
| PUT | `/api/v1/items/update` | `@CachePut` | `items-default` |
| DELETE | `/api/v1/items/clear` | `@CacheEvict` | `items-default` |
| POST | `/api/v1/items/bulk-update` | `@Caching` | evict `items-default` + put `items-conditional` |

## Pub/Sub Endpoint

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/messages/publish` | Publish a message to `items-channel` |

Request body: `{"message": "your message here"}`

The app subscribes to `items-channel` on startup — published messages appear in the app logs.

## Verify Caching

1. Call `GET /api/v1/items/standard` — first call takes ~1s (cache miss)
2. Call again — instant response (cache hit)
3. Check logs: repository method only logs on cache miss

## Verify Pub/Sub

1. `POST /api/v1/messages/publish` with `{"message": "hello"}`
2. Check app logs for `Subscriber received: hello`

Use `requests.http` to test all scenarios.
