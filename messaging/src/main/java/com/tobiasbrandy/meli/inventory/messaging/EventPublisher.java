package com.tobiasbrandy.meli.inventory.messaging;

import com.tobiasbrandy.meli.inventory.model.Event;
import com.tobiasbrandy.meli.inventory.model.EventType;

public interface EventPublisher {
    <T> Event<T> publishEvent(String stream, EventType type, T payload);

    void setDisconnected(boolean disconnected);
}
