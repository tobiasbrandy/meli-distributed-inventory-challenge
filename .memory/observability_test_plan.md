## Observability, Error Handling, and Logging Test Plan

### Goals

- Establish actionable logs, metrics, and traces for the distributed inventory prototype.
- Validate error handling contracts (HTTP, messaging) and failure modes.
- Provide practical checks runnable locally (unit/integration) and with docker-compose if time allows.

### Scope and Priorities

- API surfaces: `central-server` and `store-server` controllers and services.
- Messaging: Outbox publication and stream consumption (`OutboxEventPublisher`, `EventListener`).
- Liveness: store heartbeat production/consumption (`HeartbeatServiceImpl` in both services).

### Logging Plan

- Central `InventoryServiceImpl`:
  - INFO on item creation, purchase success (with `storeId`, `productId`, `quantity`).
  - WARN on `InsufficientStockException`, `ProductNotFoundException` contexts.
  - ERROR on DB `DataIntegrityViolationException` and unexpected exceptions.
- Store `InventoryServiceImpl`:
  - INFO on item creation and quantity updates; include resulting quantity.
  - WARN on insufficient stock; ERROR on duplicate create.
- Messaging:
  - `OutboxEventPublisher`: INFO when publishing each event id; ERROR with cause when Redis publish fails; include stream, event type, count.
  - `EventListener`: INFO after successful handler invocation (event id and type); INFO for duplicate skip; ERROR for validation failures (invalid type, handler missing, payload mismatch/parse error).
- Heartbeat:
  - Store: DEBUG on each heartbeat emit with key and timestamp.
  - Central: DEBUG when computing `isAlive` result per key and last timestamp.

Log fields should include: `storeId`, `productId`, `eventId`, `eventType`, and when relevant `quantity` or `quantityDelta`. Avoid logging payload bodies verbatim.

Verification:

- Unit tests assert logger invocations via appender spy (optional) or by behavior signals. Keep primary tests focused on behavior; spot-check logging in 1-2 key paths.

### Error Handling Policy

- REST: Map domain exceptions to RFC7807 `ProblemDetail` with clear titles; ensure validation errors return 400 with constraint messages.
- Messaging: Fail fast on invalid event type/payload; do not acknowledge processing as success. Duplicate events must be idempotently skipped.
- Outbox: On Redis failure, log ERROR and do not mark as published for entries not added; current implementation marks all; consider follow-up improvement (see Improvements backlog).

Verification:

- WebMvc tests already cover mapping of common exceptions; add cases: method not supported returns framework `ProblemDetail` body.
- Messaging tests assert invalid cases throw `IllegalArgumentException` and no handler invocation.

### Metrics Plan (to implement if time allows)

- Counters:
  - `events_published_total{type,stream}`
  - `events_processed_total{type,stream}`
  - `event_processing_errors_total{error}`
  - `purchases_total{scope=central|store, outcome=success|insufficient|not_found}`
  - `heartbeats_emitted_total{storeId}`
- Gauges:
  - `inventory_quantity{storeId,productId}` (sampled when updated)
  - `outbox_unpublished_count`
- Timers:
  - `event_publish_duration`
  - `event_handle_duration{type}`
  - `purchase_duration{scope}`

Verification:

- Add Micrometer (Prometheus registry) and assert counters/timers increment via `SimpleMeterRegistry` in unit tests for critical paths (publish, handle, purchase). If not implemented now, document as backlog.

### Tracing Plan (optional)

- Add Spring Observability / OpenTelemetry auto-config to capture HTTP spans and custom spans for `publish()` and `onMessage()`.
- Propagate trace context via Redis stream fields `traceId` (optional future enhancement).

### Failure Injection Tests

- Outbox publish: simulate Redis failure -> expect ERROR log and that unpublished events are not marked as published (requires code change; see backlog). With current code, document trade-off and assert current behavior.
- EventListener idempotency: send same event id twice -> second is skipped with INFO log.
- Heartbeat: mark disconnected on store; central purchase should yield `StoreUnavailableException` (already covered functionally); add log assertion if using appender spy.

### Operational Runbook Snippets

- Check liveness: `GET central:8081/` and `GET store:8082/`.
- Toggle network conditions: `POST store:8082/disconnected` / `POST store:8082/connected`.
- Inspect Redis keys/streams: `XRANGE streams:... - +` and `GET store:{storeId}:heartbeat`.

### Improvements Backlog

- Change outbox publishing to mark events as published only after successful XADD; retry with exponential backoff and dead-letter stream after N attempts.
- Add Micrometer metrics with Prometheus registry and sample dashboards.
- Add correlation/trace ids to logs; include `eventId` as correlation id.
- Add structured logging (JSON) with a consistent schema.
- Add health indicators for Redis connection and outbox lag gauge.
