# URL Shortener with Analytics

Full-stack home-assessment implementation of a URL shortener with analytics.

The project is split into:

- `backend` - Java 17 Spring Boot API with PostgreSQL persistence, Caffeine redirect caching, async click tracking, and tests.
- `frontend` - React + Vite UI for CRUD, strategy selection, link options, and analytics.
- `docker` - Nginx config for the packaged frontend.

## Important URLs

When running with Docker Compose:

- Frontend: `http://localhost:8081`
- Backend API: `http://localhost:8080/api`
- Redirect endpoint: `http://localhost:8080/{shortCode}`
- Strategies: `http://localhost:8080/api/strategies`
- PostgreSQL: `localhost:5434`, database/user/password all `shortener`
- Backend container DB URL: `jdbc:postgresql://postgres:5432/shortener`

When running locally for development:

- Frontend dev server: `http://localhost:5173`
- Backend API: `http://localhost:8080/api`

## Build And Run

### Docker Compose

```bash
docker compose up --build
```

Open `http://localhost:8081`.

### Backend Only

Start PostgreSQL first. The easiest path is:

```bash
docker compose up postgres
```

Then run:

```bash
mvn -pl backend spring-boot:run
```

Useful environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/shortener
SPRING_DATASOURCE_USERNAME=shortener
SPRING_DATASOURCE_PASSWORD=shortener
APP_PUBLIC_BASE_URL=http://localhost:8080
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:8081
APP_RATE_LIMIT_REDIRECTS_PER_WINDOW=120
APP_RATE_LIMIT_WINDOW_SECONDS=60
```

### Frontend Only

```bash
cd frontend
npm install
npm run dev
```

The dev server proxies `/api` to `http://localhost:8080`.

### Tests

Backend tests use H2 and do not require PostgreSQL:

```bash
mvn test
```

Frontend build check:

```bash
cd frontend
npm install
npm run test
npm run build
```

## API Examples

Create a short link with a custom alias:

```bash
curl -X POST http://localhost:8080/api/links \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://example.com/spring-campaign",
    "customAlias": "spring-sale",
    "strategy": "random_base62",
    "expiresAt": "2026-12-31T23:59:59Z",
    "maxClicks": 100,
    "tags": ["paid", "newsletter"]
  }'
```

Create a short link with an auto-generated code:

```bash
curl -X POST http://localhost:8080/api/links \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://example.com/docs",
    "strategy": "hash_truncate",
    "tags": ["docs"]
  }'
```

List links:

```bash
curl http://localhost:8080/api/links
```

View analytics:

```bash
curl http://localhost:8080/api/links/1/analytics
```

Follow a short link:

```bash
curl -i http://localhost:8080/spring-sale
```

Delete a link:

```bash
curl -X DELETE http://localhost:8080/api/links/1
```

## Backend Endpoints

- `GET /api/links` - list all short links
- `POST /api/links` - create a link
- `GET /api/links/{id}` - get one link
- `PUT /api/links/{id}` - update destination, alias, strategy, tags, expiration, and click limit
- `DELETE /api/links/{id}` - delete a link
- `GET /api/links/{id}/analytics` - total clicks, daily series, referer breakdown, user-agent breakdown
- `GET /api/strategies` - read-only generation strategies and parameter schema
- `GET /{shortCode}` - public redirect endpoint

Redirect status codes:

- `302 Found` for an active link
- `404 Not Found` for a missing code
- `410 Gone` for an expired link
- `429 Too Many Requests` for a click-exhausted link
- `429 Too Many Requests` with `Retry-After` when a client exceeds the redirect rate limit

## Design Decisions

- Redirect reads are cached with Caffeine under the `shortLinks` cache to keep the public path fast.
- Public redirects are rate limited per client IP before cache/database resolution. The default is 120 redirect attempts per 60 seconds and can be configured with `APP_RATE_LIMIT_REDIRECTS_PER_WINDOW` and `APP_RATE_LIMIT_WINDOW_SECONDS`.
- Click tracking is async, so the redirect response is not blocked by analytics persistence.
- Cache entries are evicted after CRUD changes. Click tracking only evicts click-limited links, keeping `maxClicks` checks current without invalidating unlimited links on every click.
- PostgreSQL is the production database in Compose; H2 is used for repeatable integration tests.
- Strategies are predefined and read-only. The UI can select them, but cannot create or edit strategies.
- `expiresAt`, `maxClicks`, and `tags` are modeled as link parameters and returned in the strategy schema.

## Scalability Considerations

The current design separates the high-volume public redirect path from the lower-volume management and analytics API. Public redirects hit the `shortLinks` Caffeine cache by short code, then immediately return a `302` response while click analytics are written asynchronously. This keeps redirect latency low and avoids making users wait for analytics persistence.

This implementation can be scaled horizontally by running multiple backend instances behind a load balancer, because application state is stored in PostgreSQL. Each instance has its own local Caffeine cache, so CRUD operations only evict cache entries on the instance that processed the change. For a multi-instance production deployment, replace local cache eviction with a shared cache such as Redis, or publish invalidation events so every node drops stale redirect targets.

The main write bottleneck is click tracking. Every click currently creates a `click_events` row and increments the link's persisted `clickCount`. For higher traffic, a better design would buffer click events through a queue such as Kafka, RabbitMQ, or SQS, process them in workers, and batch analytics writes. Click counters can move to Redis atomic increments with periodic PostgreSQL flushes, especially for `maxClicks` enforcement.

PostgreSQL should be indexed and partitioned around analytics access patterns as data grows. The current `click_events(short_link_id, clicked_at)` index supports per-link analytics; larger deployments should consider time-based partitioning, retention policies, materialized daily aggregates, and read replicas for dashboard queries.

The frontend polling is intentionally simple and suitable for an assessment-sized app. At larger scale, dashboards should use longer polling intervals, server-sent events, WebSockets, or pre-aggregated analytics endpoints to avoid many clients repeatedly querying raw data.

Implemented strategies:

- `random_base62` - random 7-character Base62 code.
- `hash_truncate` - SHA-256 hash truncation of the destination URL.
- `sequential_base62` - monotonically increasing Base62 counter.
- Custom alias - supplied through `customAlias`; must be unique.

## Assumptions

- The application is single-tenant and does not include authentication.
- Tags are stored as a simple collection for filtering and display, not as a separate tag-management system.
- Click limiting is enforced from the persisted count cached in the redirect target. The async tracker evicts click-limited links after each click; unlimited links remain cacheable. A high-volume production system could move this counter to Redis with atomic increments.
- IP geo enrichment, QR generation, and redirect rate limiting are left as documented bonus extensions.

## AI Tools Used

This solution was created with OpenAI Codex. Codex was used to read the assessment PDF, generate the Spring Boot backend, React frontend, Docker setup, tests, and this README, then run local verification and fix issues found during that pass.
