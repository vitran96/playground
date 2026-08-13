# Spring Boot 4 + Resilience4j: Circuit Breaker & Rate Limiter Sample

Demonstrates **Circuit Breaker** and **Rate Limiter** patterns using Spring Boot 4, Java 25, Resilience4j 2.4.0, and OkHttp3.

## Tech Stack

| Component | Version |
|-----------|---------|
| Java | 25 |
| Spring Boot | 4.0.0 |
| Resilience4j | 2.4.0 (`resilience4j-spring-boot4`) |
| OkHttp | 5.4.0 |
| Build | Gradle |

## Architecture

```
Client → /api/client/*  (CircuitBreaker / RateLimiter via Resilience4j)
           ↓ OkHttp3
         /api/mock/external/*  (simulated downstream services)
```

### Mock Downstream Endpoints

| Endpoint | Behavior |
|----------|----------|
| `GET /api/mock/external/always-fail` | Always returns 500 |
| `GET /api/mock/external/flaky` | Fails every 5th call (500), rest return 200 |
| `GET /api/mock/external/healthy` | Always returns 200 |

### Client Endpoints (Resilience4j-protected)

| Endpoint | Pattern | Resilience4j Instance |
|----------|---------|----------------------|
| `GET /api/client/call-failing` | Circuit Breaker | `alwaysFailingService` |
| `GET /api/client/call-flaky` | Circuit Breaker | `flakyService` |
| `GET /api/client/call-healthy` | Circuit Breaker | `healthyService` |
| `GET /api/client/rate-limited` | Rate Limiter | `rateLimitedService` |

## Run

```bash
gradle bootRun
```

## Test Scenarios (curl)

### 1. Circuit Breaker — Always Failing Service

```bash
# Repeat to observe: first calls fail → breaker opens → fallback returned instantly
for i in $(seq 1 10); do
  curl -s http://localhost:8080/api/client/call-failing
  echo
done
```

Expected: all return `{"status":"Fallback: Downstream service is currently unavailable"}`

### 2. Circuit Breaker — Flaky Service

```bash
# Intermittent failures may trip the breaker after enough errors
for i in $(seq 1 15); do
  curl -s http://localhost:8080/api/client/call-flaky
  echo
done
```

### 3. Circuit Breaker — Healthy Service

```bash
# Breaker stays CLOSED, all calls succeed
for i in $(seq 1 10); do
  curl -s http://localhost:8080/api/client/call-healthy
  echo
done
```

### 4. Rate Limiter (5 req / 10s)

```bash
# First 5 succeed, 6th gets 429
for i in $(seq 1 7); do
  curl -s -w "\nHTTP %{http_code}\n" http://localhost:8080/api/client/rate-limited
done
```

## Configuration

See `src/main/resources/application.yml` for all Resilience4j settings (sliding window sizes, thresholds, rate limits).

## Tests

```bash
gradle test
```

Verifies circuit breaker fallback, healthy service passthrough, and rate limiter 429 responses.
