## Test Strategy

Prioritize correctness of distributed flow, consistency, and error handling. Use module-level tests with Spring Boot test slices and unit tests where feasible. Avoid heavy end-to-end docker dependencies; use embedded H2 and mock Redis where necessary.

### Tools/Frameworks

- JUnit 5, Spring Boot Test, Mockito.
- For Redis interactions, prefer mocking `StringRedisTemplate` and testing logic in isolation. Optionally, Testcontainers for Redis (time permitting).

### Modules & Scope

#### messaging

1. OutboxEventPublisher

   - publishes event: type/payload validation, serialization errors -> IllegalArgumentException.
   - persistence: saves `OutboxEvent` with correct fields.
   - scheduled publish()
     - when `disconnected = true`: does not publish to Redis, does not mark published.
     - happy path: writes XADD with expected map; marks events as published.
     - redis failure: logs error; still marks published? (Current code always calls `markPublished`; assert that entries are marked published even on exception — or adjust expectations to current behavior.)

2. EventListener

   - idempotency: when `SETNX` returns false, handler not invoked.
   - invalid event type -> IllegalArgumentException.
   - handler not found -> IllegalArgumentException.
   - payload type mismatch -> IllegalArgumentException.
   - happy path: deserializes and invokes handler once.

3. MessagingConfig
   - create consumer group if missing; ignores if exists. Use mocked `StringRedisTemplate` ops and verify `createGroup` call wrapped in try/catch.

#### repository

1. InventoryRepository.updateQuantity

   - updates existing row and returns 1.
   - non-existing returns 0.

2. OutboxEventRepository.findByPublishedFalseOrderByIdAsc
   - returns ordered page of unpublished events.
   - markPublished updates rows; returns count.

#### model

1. Entities mapping smoke tests
   - persist and retrieve `InventoryItem` unique constraint on (storeId, productId).
   - persist `OutboxEvent` constructor populates fields.

#### store-server

1. InventoryServiceImpl

   - createInventoryItem: saves, publishes CREATED; duplicate -> ProductAlreadyExistsException.
   - setInventoryItemQuantity: updates count, publishes UPDATED; missing -> ProductNotFoundException.
   - processPurchase: sufficient stock -> decrements, publishes UPDATED with new quantity; insufficient -> InsufficientStockException.
   - listInventoryItems: delegates to repo with PageRequest.

2. InventoryController (WebMvcTest)

   - GET / health returns message.
   - GET /inventory paginates defaults and bounds validation.
   - GET /inventory/{productId} -> 200 with body; not found -> 404 ProblemDetail.
   - POST /inventory with invalid/missing productId -> 400; duplicate -> 409; success -> 201 with body.
   - PUT /inventory/{productId} with invalid quantity -> 400; missing -> 404; success -> 204.
   - POST /purchase/{productId} invalid quantity -> 400; insufficient -> 400; success -> 200 with body.
   - /connected and /disconnected toggle flags and return strings.

3. HeartbeatServiceImpl (store)

   - emitHeartbeat writes timestamp to expected key.
   - scheduledHeartbeat skips when disconnected.

4. InventoryEventHandler
   - wiring: handler for REMOTE_PURCHASE invokes service with expected args.

#### central-server

1. InventoryServiceImpl

   - getInventoryItem: returns or throws ProductNotFoundException.
   - createInventoryItem: saves; duplicate -> ProductAlreadyExistsException.
   - setInventoryItemQuantity: updates or throws ProductNotFoundException.
   - processPurchase:
     - store unavailable -> StoreUnavailableException (uses HeartbeatService.isAlive=false).
     - not found -> ProductNotFoundException.
     - insufficient -> InsufficientStockException.
     - success -> decrements, saves, publishes REMOTE_PURCHASE event to correct stream.
   - listInventoryItems: delegates to repo with PageRequest.

2. InventoryController (WebMvcTest)

   - GET / health.
   - GET /inventory pagination params and defaults.
   - GET /inventory/{storeId}/{productId}: invalid storeId -> 400; not found -> 404; success -> 200.
   - POST /purchase/{storeId}/{productId} body validation; expected error mappings; success -> 200.

3. HeartbeatServiceImpl (central)

   - isAlive true when timestamp within 70s; false otherwise; missing key -> false.

4. InventoryEventHandler
   - CREATED and UPDATED handlers invoke service with expected args.

### Concurrency and Consistency

- Add a transactional test to ensure `updateQuantity` flush/clear works and reflects persisted state.
- Optional: simulate concurrent purchases with optimistic approach (not currently implemented). Keep scope to verifying service logic path.

### Test Data & Fixtures

- Use factory helpers for `InventoryItem` and events.
- Leverage H2 in-memory DB auto-configured by Spring Boot tests.

### Out of Scope (for now)

- Full end-to-end with real Redis streams and two running apps.
- Store-to-store events beyond defined handlers.

### Coverage Goals

- messaging: ~90% classes/branches.
- services/controllers: ~85% lines with key branches covered.
- repositories/entities: smoke + critical paths.
