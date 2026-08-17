# Microservices Order System

Three small Spring Boot services demonstrating a REST-based microservices architecture: an API
gateway routes traffic, an order service calls an inventory service synchronously to reserve stock
before confirming an order, and each service owns its own database.

## Architecture

```
                 ┌───────────┐
   client  ───▶  │  gateway  │  (Spring Cloud Gateway, port 8080)
                 └─────┬─────┘
              ┌────────┴────────┐
              ▼                 ▼
     ┌────────────────┐  ┌────────────────┐
     │ inventory-      │  │ order-service   │
     │ service (8082)  │◀─┤ (8083)          │
     │                 │  │  WebClient call │
     │ own Postgres DB │  │ own Postgres DB │
     └────────────────┘  └────────────────┘
```

- **gateway** — Spring Cloud Gateway with static path-based routing (`/api/products/**` →
  inventory-service, `/api/orders/**` → order-service). No service discovery is used; routes are
  configured directly, which is a common, simpler alternative to Eureka for small systems.
- **inventory-service** — owns `Product` stock levels. Exposes `POST /api/products/{id}/reserve`,
  which atomically decrements stock or returns `409 Conflict` if there isn't enough.
- **order-service** — owns `Order` records. On `POST /api/orders`, it calls inventory-service's
  reserve endpoint via `WebClient`; a successful reservation confirms the order, a `409`/`404`
  from inventory-service is translated into an equivalent response and the order is persisted as
  `REJECTED` for audit purposes.

Each service owns its own PostgreSQL database — no shared schema — which is why an order can be
"confirmed" only via a network call to inventory-service rather than a local transaction.

## Running locally

Copy `.env.example` to `.env` and fill in real values (`.env` is gitignored):

```bash
cp .env.example .env
docker compose up --build
```

This starts two Postgres instances, `inventory-service` (8082), `order-service` (8083), and
`gateway` (8080). Everything after that goes through the gateway:

```bash
curl -X POST localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-1","name":"Mechanical Keyboard","stock":10}'

curl -X POST localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":3}'
```

To run a single service against the others locally (e.g. for development), start its Postgres
container from the root `docker-compose.yml` and run `./gradlew :order-service:bootRun` etc.; each
service's `application.yml` defaults its peer URLs to `localhost`.

## Tests

```bash
./gradlew build
```

- `inventory-service` — Mockito unit test for the stock-reservation rule, a `@WebMvcTest` slice,
  and a Testcontainers integration test against real PostgreSQL
- `order-service` — Mockito unit test for the confirm/reject branching logic, a `@WebMvcTest`
  slice, and an integration test combining Testcontainers (Postgres) with OkHttp's `MockWebServer`
  standing in for inventory-service — demonstrating how to test an HTTP client dependency without
  running the real downstream service
- `gateway` — a `WebTestClient`-based test asserting each path prefix is routed to the correct
  stubbed backend

## CI

`.github/workflows/ci.yml` runs `./gradlew build` (all three modules, all tests) on every push and
PR — GitHub Actions' Ubuntu runners ship Docker, so the Testcontainers tests run there too.
