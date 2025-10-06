package com.tobiasbrandy.meli.inventory.messaging;

import com.tobiasbrandy.meli.inventory.model.Event;
import com.tobiasbrandy.meli.inventory.model.EventType;

import java.util.function.Consumer;

public interface EventHandler<T> {
    EventType eventType();
    Class<T> payloadType();
    void handleEvent(Event<T> event);

    static <T> EventHandler<T> of(final EventType eventType, final Class<T> payloadType, final Consumer<Event<T>> handler) {
        return new EventHandler<>() {
            @Override
            public EventType eventType() {
                return eventType;
            }

            @Override
            public Class<T> payloadType() {
                return payloadType;
            }

            @Override
            public void handleEvent(Event<T> event) {
                handler.accept(event);
            }
        };
    }

    static <T> EventHandler<T> ofPayload(final EventType eventType, final Class<T> payloadType, final Consumer<T> handler) {
        return of(eventType, payloadType, event -> handler.accept(event.payload()));
    }
}
