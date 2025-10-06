## Overview

A multi-module Spring Boot prototype modeling a distributed inventory system with a central server and store servers, using Redis Streams for messaging and H2 for persistence. Modules: `central-server`, `store-server`, `messaging`, `repository`, and `model`.

## Architecture

- Central and store services are independent Spring Boot apps with scheduling enabled.
- Persistence via JPA/H2 in-memory DB. Entities: `InventoryItem`, `OutboxEvent`.
- Messaging via Redis Streams with consumer groups; outbox pattern for reliable publishes.
- Config via `application.yml` (service) and shared `messaging.yml` (Redis, stream names).

## Domain Model

- `InventoryItem(id, storeId, productId, quantity)` with unique (storeId, productId).
- Events:
  - `INVENTORY_ITEM_CREATED(InventoryItemCreateEvent)`
  - `INVENTORY_ITEM_UPDATED(InventoryItemUpdateEvent)`
  - `INVENTORY_ITEM_REMOTE_PURCHASE(InventoryItemRemotePurchaseEvent)`
- Exceptions: `ProductNotFoundException`, `ProductAlreadyExistsException`, `InsufficientStockException`, `InvalidStoreIdException`, `StoreUnavailableException`.

## Repository Layer

- `InventoryRepository` with `findByStoreIdAndProductId` and bulk `updateQuantity` JPQL.
- `OutboxEventRepository` with `findByPublishedFalseOrderByIdAsc(Pageable)` and `markPublished(ids)`.

## Messaging

- `EventStreams` builds stream keys (with `{storeId}` placeholders).
- `EventPublisher` implemented by `OutboxEventPublisher`:
  - Validates payload type matches `EventType`.
  - Serializes payload with Jackson; stores `OutboxEvent` in DB.
  - Scheduled `publish()` reads unpublished outbox events (page size 10), posts to Redis stream (skip if `disconnected`), then marks as published.
- `EventListener` subscribes to configured streams via `MessagingConfig` and:
  - Performs idempotency check using Redis `SETNX` with eventId key.
  - Deserializes payload based on `EventType`, looks up handler, invokes it.

## Store Server

- `AppConfig(storeId)`; server port 8082.
- `InventoryController` endpoints:
  - `GET /` health.
  - `POST /connected` / `POST /disconnected` toggles heartbeat/publisher `disconnected` flags.
  - `GET /inventory?page&size` list (defaults: page=0, size=20, max size=1000).
  - `GET /inventory/{productId}` fetch.
  - `POST /inventory {productId}` create -> publishes `INVENTORY_ITEM_CREATED`.
  - `PUT /inventory/{productId} {quantity}` set -> publishes `INVENTORY_ITEM_UPDATED`.
  - `POST /purchase/{productId} {quantity}` local purchase -> updates quantity and publishes `INVENTORY_ITEM_UPDATED` with new quantity.
- `InventoryServiceImpl` performs JPA ops and publishes events via `EventPublisher`/`EventStreams`.
- `HeartbeatServiceImpl` (store) writes current timestamp to Redis key `store:{storeId}:heartbeat` every 30s (skips when disconnected).
- Exception handling via `GlobalExceptionHandler` mapping domain errors to HTTP statuses.
- `InventoryEventHandler` registers handler to apply `INVENTORY_ITEM_REMOTE_PURCHASE` by decrementing local inventory.

## Central Server

- `AppConfig(stores: List<String>)`; server port 8081.
- `InventoryController` endpoints:
  - `GET /` health.
  - `GET /inventory?page&size` list.
  - `GET /inventory/{storeId}/{productId}` fetch after `storeId` validation against configured stores.
  - `POST /purchase/{storeId}/{productId} {quantity}` remote purchase:
    - Checks `HeartbeatService.isAlive(storeId)`; if false -> `StoreUnavailableException`.
    - Validates existence and stock; decrements quantity; persists; publishes `INVENTORY_ITEM_REMOTE_PURCHASE` to that store.
- `InventoryServiceImpl` handles create/update/list/read for central DB.
- `HeartbeatServiceImpl` (central) reads Redis `store:{storeId}:heartbeat` and considers alive if within 70s.
- `InventoryEventHandler` wires handlers for `INVENTORY_ITEM_CREATED` and `INVENTORY_ITEM_UPDATED` to mirror store changes into central DB.
- Exception handling via central `GlobalExceptionHandler`.

## Non-functional

- Input validation with Jakarta Validation annotations.
- Idempotent event processing; strict payload-type matching for safety.
- Outbox pattern for reliability and eventual delivery; optional disconnection simulation.
