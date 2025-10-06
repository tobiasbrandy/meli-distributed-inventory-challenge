package com.tobiasbrandy.meli.inventory.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tobiasbrandy.meli.inventory.model.Event;
import com.tobiasbrandy.meli.inventory.model.EventType;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Stream listener that delegates events to all {@link EventHandler} beans.
 * <p>
 * Ensures idempotency via Redis SETNX on the event id, validates event type and
 * payload.
 */
@Slf4j
@Component
public class EventListener implements StreamListener<String, MapRecord<String, String, String>> {
    private final Map<EventType, EventHandler<?>> handlers;
    private final ObjectMapper mapper;
    private final StringRedisTemplate redis;

    public EventListener(
        final List<EventHandler<?>> handlers,
        final ObjectMapper mapper,
        final StringRedisTemplate redis
    ) {
        this.mapper = mapper;
        this.redis = redis;

        this.handlers = new ConcurrentHashMap<>(handlers.size());
        for (EventHandler<?> h : handlers) {
            this.handlers.put(h.eventType(), h);
        }
    }

    @Override
    public void onMessage(final MapRecord<String, String, String> msg) {
        val stream = msg.getStream();
        val eventMap = msg.getValue();
        val eventId = eventMap.get("id");
        val createdAt = Instant.parse(eventMap.get("createdAt"));

        // Idempotency check
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(eventId, eventId))) {
            log.info("Skipping duplicate event {}", eventId);
            return;
        }

        final EventType type;
        try {
            type = EventType.valueOf(eventMap.get("type"));
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid event type");
        }

        val eventHandler = handlers.get(type);
        if (eventHandler == null) {
            throw new IllegalArgumentException("Event type not found");
        }
        if (type.getPayloadType() != eventHandler.payloadType()) {
            throw new IllegalArgumentException("Event type does not match handler payload type");
        }

        final Object payload;
        try {
            payload = mapper.readValue(eventMap.get("payload"), type.getPayloadType());
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize event payload", e);
        }

        @SuppressWarnings("unchecked")
        final EventHandler<Object> rawHandler = (EventHandler<Object>) eventHandler;
        final Event<Object> event = new Event<>(stream, eventId, createdAt, type, payload);
        rawHandler.handleEvent(event);

        log.info("Processed event {}", event);
    }
}
