package com.tobiasbrandy.meli.inventory.messaging;

import com.tobiasbrandy.meli.inventory.model.Event;
import com.tobiasbrandy.meli.inventory.model.EventType;

/**
 * Service for publishing domain events to a stream.
 */
public interface EventPublisher {
    <T> Event<T> publishEvent(String stream, EventType type, T payload);

    void setDisconnected(boolean disconnected);
}
