## Metrics Plan

We won’t implement metrics now, but this document defines a concrete set of metrics for future work, aligned with incident response and product insights.

### Principles

- Prefer low-cardinality labels (limit to `storeId`, `productId` only where necessary).
- Counters for throughput/errors; timers for latency; gauges for current state.
- Metrics should answer: Are events flowing? Are purchases succeeding? Is replication healthy?

### Counters

- events_published_total{type,stream}
  - Tracks total events persisted to outbox (business throughput).
- events_streamed_total{type,stream}
  - Tracks events successfully XADD’d to Redis (delivery throughput).
- events_processed_total{type,stream}
  - Number of events processed by consumers (consumption throughput).
- event_processing_errors_total{type,error}
  - Failures in `EventListener` (invalid type, handler missing, payload parse).
- purchases_total{scope=central|store,outcome=success|insufficient|not_found}
  - Business outcomes of purchases across services.
- heartbeats_emitted_total{storeId}
  - Liveness signals emitted by stores.

### Timers

- event_publish_duration
  - Time to serialize and persist to outbox.
- event_stream_add_duration
  - Time to XADD to Redis.
- event_handle_duration{type}
  - Time to handle an event (from receipt to completion).
- purchase_duration{scope}
  - Time to execute a purchase path.

### Gauges

- outbox_unpublished_count
  - Backlog size indicating delayed delivery.
- inventory_quantity{storeId,productId}
  - Optional; sample only on update to avoid churn; useful for dashboards.
- store_alive{storeId}
  - Derived gauge (1/0) via heartbeat freshness.

### Dashboards (sketch)

- Messaging Health
  - events_published_total vs events_streamed_total vs events_processed_total
  - outbox_unpublished_count (per service)
  - event_processing_errors_total by type
- Purchase Funnel
  - purchases_total by outcome, split by scope
  - purchase_duration p50/p90/p99
- Liveness
  - store_alive by storeId
  - heartbeats_emitted_total rate

### Alerts (examples)

- High outbox backlog (outbox_unpublished_count > threshold for 5m)
- Event processing errors spike (rate > baseline)
- Zero processed events for > 5m during business hours

### Notes

- Revisit label cardinality before enabling `productId` at scale; consider sampling.
