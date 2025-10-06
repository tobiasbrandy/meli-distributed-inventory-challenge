package com.tobiasbrandy.meli.inventory.model;

import java.time.Instant;

public record Event<T>(
    String stream,
    String id, // UUID
    Instant createdAt,
    EventType type,
    T payload
) {
}
