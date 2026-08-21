# Microservices Order System

So here instead of a monolith, made a separated in different microservices. 
Normally we would use a traditional controller, but instead a gateway routes traffic to the other 2 microservices.
An order service calls an inventory service synchronously to reserve stock before
confirming an order, and each service has its own database.

## Architecture

```
                 
   client  ───>  │  gateway  │  (Spring Cloud Gateway, port 8080)
                        |
              ---------------------
              |                   |
     -----------------     ----------------
     │ inventory-     │  │ order-service  │
     │ service (8082) │<─┤ (8083)         │
     │                │  │  WebClient call│
     │ own Postgres DB│  │ own Postgres DB│
     ----------------     ----------------
```

- **gateway** — routes by path only (`/api/products/**` → inventory-service, `/api/orders/**` →
  order-service). No service discovery, routes hardcoded, simpler for 2 services.
- **inventory-service** — owns stock. `POST /api/products/{id}/reserve` decrements stock in one
  transaction, 409s if there's not enough.
- **order-service** — owns `Order`s. `POST /api/orders` calls inventory-service's reserve endpoint
  first; success confirms the order, a 404/409 gets passed through and the order is saved as
  `REJECTED` instead.

Each service has its own Postgres — confirming an order needs an HTTP call.

## Running it

Copy `.env.example` to `.env` and fill in real values (`.env` is gitignored):

```bash
cp .env.example .env
docker compose up --build
```

Wakes 2 Postgres DB, `inventory-service` (8082), `order-service` (8083), `gateway`
(8080). Talk to it through the gateway:

```bash
curl -X POST localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-1","name":"Mechanical Keyboard","stock":10}'

curl -X POST localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":3}'
```

To run one service against the others locally, start its Postgres from the root
`docker-compose.yml` and run e.g. `./gradlew :order-service:bootRun`. Peer URLs default to
`localhost`.

## Tests

```bash
./gradlew build
```

- `inventory-service` — Mockito unit test, `@WebMvcTest` slice, Testcontainers integration test
- `order-service` — Mockito unit test, `@WebMvcTest` slice, integration test pairing
  Testcontainers (Postgres) with OkHttp's `MockWebServer` standing in for inventory-service
- `gateway` — `WebTestClient` test checking each path routes to the right backend

## CI

`.github/workflows/ci.yml` runs `./gradlew build` for every push and PR.
