# Spring Boot + OpenTelemetry + LGTM Stack

A minimal Spring Boot 4 application instrumented with OpenTelemetry, sending **logs, metrics, and traces** to a Grafana LGTM stack — all running with a single `docker compose up`.

## What Is This?

A ready-to-run observability demo that wires together:

| Component | Role |
|---|---|
| **Spring Boot 4** (Java 25) | Sample REST API |
| **OpenTelemetry Java Agent** | Zero-code instrumentation (traces, metrics, log bridging) |
| **Grafana LGTM** (all-in-one) | Loki (logs) + Grafana (dashboards) + Tempo (traces) + Mimir (metrics) |

The app exposes two endpoints — one happy path, one error path — so you can see how successes, failures, and correlation IDs flow through the entire observability pipeline.

## Why Use This?

**Problem:** You want to add observability to a Spring Boot app but the setup guides are fragmented — one for traces, one for metrics, one for logs, each with different exporters and backends.

**Solution:** This project gives you the full loop in one place:

```
HTTP Request → Spring Boot → OTel Java Agent → OTLP → Loki/Tempo/Mimir → Grafana
```

Every request gets a **correlation ID** (`X-Correlation-Id` header) that appears in console logs, exported logs, and can be used to jump from a log line in Loki to its trace in Tempo.

## Quick Start

### Docker (recommended)

```bash
docker compose up --build
```

- **App:** http://localhost:8080/api/hello
- **Error endpoint:** http://localhost:8080/api/error
- **Grafana:** http://localhost:3000 (login: `admin` / `admin`)

In Grafana, the data sources (Loki, Tempo, Mimir) are pre-configured. Go to **Explore** and pick any of them.

### Local Development

```bash
./gradlew bootRun
```

Runs with the `local` Spring profile (set in the project root `application.properties`), which enables **colored console output**. No OTel export happens locally unless you run a collector yourself.

## Project Structure

```
├── application.properties          # Project root — activates 'local' profile for colored logs
├── build.gradle                    # Spring Boot 4, Log4j2, no extra deps
├── docker-compose.yml              # 2 services: app + grafana/otel-lgtm
├── Dockerfile                      # Multi-stage build + OTel Java agent download
└── src/main/
    ├── java/com/example/myapp_backend/
    │   ├── Application.java        # Spring Boot entry point
    │   ├── CorrelationFilter.java  # X-Correlation-Id + request/response logging
    │   └── SampleController.java   # GET /api/hello (200), GET /api/error (500)
    └── resources/
        ├── application.yml         # Server port, app name
        └── log4j2-spring.xml       # Console logging with correlation ID, color toggle
```

## How It Works

### Instrumentation: OTel Java Agent

The [OpenTelemetry Java Agent](https://opentelemetry.io/docs/zero-code/java/agent/) is attached via `-javaagent` in the Dockerfile. It **automatically** instruments:

- **HTTP server spans** — every incoming request gets a trace
- **JVM metrics** — memory, GC, threads, etc.
- **Log bridging** — Log4j2 log events are captured and exported as OTel log records

No SDK code in the application. Zero lines of OTel dependency in `build.gradle`.

### Correlation ID

The `CorrelationFilter` servlet filter:

1. Reads `X-Correlation-Id` from the request header (or generates a UUID)
2. Puts it in SLF4J MDC → appears in every log line as `[correlationId]`
3. Sets it on the response header
4. Logs `GET /api/hello` on request, `200 GET /api/hello` on response

### Console Color

Controlled by Spring profile via `<SpringProfile>` in `log4j2-spring.xml`:

| Environment | Profile | Console Output |
|---|---|---|
| Local (`./gradlew bootRun`) | `local` | Colored (ANSI `%highlight`) |
| Docker | _(none)_ | Plain text |

The root `application.properties` sets `spring.profiles.active=local`. Docker doesn't mount this file, so no profile is active → plain output.

### LGTM Stack

Uses [`grafana/otel-lgtm`](https://hub.docker.com/r/grafana/otel-lgtm) — a single Docker image that bundles:

- **OpenTelemetry Collector** — receives OTLP on ports 4317 (gRPC) and 4318 (HTTP)
- **Loki** — log aggregation
- **Tempo** — distributed tracing
- **Mimir** — metrics storage (Prometheus-compatible)
- **Grafana** — visualization with all data sources pre-provisioned

## Benefits

- **Zero application code for telemetry** — the Java agent does all instrumentation at the bytecode level
- **Single `docker compose up`** — full observability stack in two containers
- **Correlated debugging** — correlation ID links logs ↔ traces across the pipeline
- **Batteries included** — Grafana data sources pre-wired, no manual provisioning
- **Production-like signals** — same telemetry format (OTLP) and backends you'd use in production

## Pros & Cons

### Pros

| | |
|---|---|
| **Zero-code instrumentation** | OTel Java agent instruments HTTP, JDBC, gRPC, messaging, etc. without touching your code |
| **All three signals** | Logs, metrics, and traces from one agent, one protocol (OTLP), one stack |
| **Easy to extend** | Add a database, message queue, or HTTP client — the agent auto-instruments it |
| **Vendor-neutral** | OTLP is an open standard; swap Grafana stack for Datadog, Jaeger, or Elastic with config changes |
| **Minimal dependencies** | Only `spring-boot-starter-webmvc` and `spring-boot-starter-log4j2` in `build.gradle` |

### Cons

| | |
|---|---|
| **Java agent overhead** | ~50-100ms added startup time, minor memory overhead (~30-50MB); not an issue for long-running services |
| **All-in-one image** | `grafana/otel-lgtm` is great for dev but not for production (no HA, no persistence by default, can't scale components independently) |
| **Agent version coupling** | Agent updates can change what's instrumented or how; pin the version in production |
| **Log4j2 appender gap** | Spring Boot 4 doesn't auto-configure the OTel Log4j2 appender — we rely on the Java agent's log bridging instead |
| **No custom metrics** | This demo only captures auto-generated metrics; adding business metrics requires Micrometer or OTel SDK code |

## Alternatives

### Instrumentation Approach

| Approach | Trade-off |
|---|---|
| **OTel Java Agent** _(this project)_ | Zero code, broad auto-instrumentation. Some startup cost. |
| **Spring Boot OTel Starter** (`spring-boot-starter-opentelemetry`) | SDK-based, tighter Spring integration, more control. Requires explicit log appender config for Log4j2. |
| **Micrometer + Micrometer Tracing** | Spring-native metrics/tracing. No log export. Need separate log pipeline. |
| **Grafana Alloy** (sidecar) | Collects logs from files/stdout + scrapes Prometheus metrics. No in-process traces. |

### Observability Backend

| Backend | Trade-off |
|---|---|
| **Grafana LGTM** _(this project)_ | Free, self-hosted, all-in-one. Must operate yourself in prod. |
| **Grafana Cloud** | Managed LGTM. Free tier available. Same query UX, no ops. |
| **Datadog / New Relic / Dynatrace** | Fully managed, richer APM features. Expensive at scale. Vendor lock-in. |
| **Jaeger + Prometheus + ELK** | Open-source, battle-tested individually. More moving parts to integrate. |
| **AWS X-Ray + CloudWatch** | Native on AWS. Tightly coupled to AWS ecosystem. |

### Log Framework

| Framework | Trade-off |
|---|---|
| **Log4j2** _(this project)_ | Async loggers, structured logging, flexible config. Requires excluding Spring Boot default logging. |
| **Logback** (Spring Boot default) | Zero config, well-supported. Log4j2 is faster for async-heavy workloads. |
| **JUL (java.util.logging)** | Built-in. Inferior API and performance. |

## Next Steps

To take this toward production:

1. **Pin the OTel agent version** in the Dockerfile instead of using `latest`
2. **Replace `grafana/otel-lgtm`** with individual Loki/Tempo/Mimir containers (or Grafana Cloud)
3. **Add custom metrics** via Micrometer for business KPIs
4. **Add structured logging** (JSON layout in Log4j2) for better Loki querying
5. **Add health checks** (`spring-boot-starter-actuator`) for container orchestration
