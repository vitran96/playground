# Spring Boot Caffeine Cache Sample

Spring Boot 4 + Caffeine cache demo. Shows `@Cacheable` / `@CacheEvict` with a configured `CaffeineCacheManager`.

## Stack

- Java 25, Spring Boot 4.x, Gradle
- `spring-boot-starter-cache` + `caffeine`

## Run

```bash
gradle bootRun
```

## Endpoints

| Method | Path         | Description                        |
|--------|--------------|------------------------------------|
| GET    | /api/items   | List items (cached for 10 min)     |
| POST   | /api/items   | Add item & evict cache             |

POST body: `{"name": "Orange"}`

First GET is slow (~2 s simulated delay). Subsequent GETs are instant until cache expires or a POST evicts it.

## Cache Config

`CacheConfig.java` — single Caffeine cache named `itemsCache`:
- Initial capacity: 100
- Max size: 500
- Expire after write: 10 minutes
- Stats recording enabled

## Test

```bash
gradle test
```

Two integration tests verify cache-hit speed and eviction-on-add behavior.
