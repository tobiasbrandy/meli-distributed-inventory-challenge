package com.tobiasbrandy.meli.inventory.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tobiasbrandy.meli.inventory.model.Event;
import com.tobiasbrandy.meli.inventory.model.EventType;
import com.tobiasbrandy.meli.inventory.model.InventoryItemCreateEvent;
import com.tobiasbrandy.meli.inventory.model.InventoryItemUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventListenerTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private ObjectMapper mapper;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        mapper = new ObjectMapper();
    }

    @Test
    void skipsDuplicateEvents() {
        var counter = new AtomicInteger(0);
        var handler = EventHandler.ofPayload(EventType.INVENTORY_ITEM_CREATED, InventoryItemCreateEvent.class,
                e -> counter.incrementAndGet());
        var listener = new EventListener(java.util.List.of(handler), mapper, redis);

        when(valueOps.setIfAbsent(anyString(), anyString())).thenReturn(false);

        var record = MapRecord.create("stream", Map.of(
                "id", "event-1",
                "createdAt", Instant.now().toString(),
                "type", EventType.INVENTORY_ITEM_CREATED.name(),
                "payload", "{\"storeId\":\"s\",\"productId\":\"p\"}"));

        listener.onMessage(record);

        assertEquals(0, counter.get());
    }

    @Test
    void throwsOnInvalidEventType() {
        var listener = new EventListener(java.util.List.of(), mapper, redis);
        when(valueOps.setIfAbsent(anyString(), anyString())).thenReturn(true);

        var record = MapRecord.create("stream", Map.of(
                "id", "event-1",
                "createdAt", Instant.now().toString(),
                "type", "UNKNOWN",
                "payload", "{}"));

        assertThrows(IllegalArgumentException.class, () -> listener.onMessage(record));
    }

    @Test
    void throwsWhenHandlerNotFound() {
        var listener = new EventListener(java.util.List.of(), mapper, redis);
        when(valueOps.setIfAbsent(anyString(), anyString())).thenReturn(true);

        var record = MapRecord.create("stream", Map.of(
                "id", "event-1",
                "createdAt", Instant.now().toString(),
                "type", EventType.INVENTORY_ITEM_CREATED.name(),
                "payload", "{\"storeId\":\"s\",\"productId\":\"p\"}"));

        assertThrows(IllegalArgumentException.class, () -> listener.onMessage(record));
    }

    @Test
    void throwsWhenHandlerPayloadTypeMismatch() {
        // Mismatch: handler registers CREATED with UpdateEvent class
        var badHandler = EventHandler.of(EventType.INVENTORY_ITEM_CREATED, InventoryItemUpdateEvent.class,
                (Event<InventoryItemUpdateEvent> e) -> {
                });
        var listener = new EventListener(java.util.List.of(badHandler), mapper, redis);
        when(valueOps.setIfAbsent(anyString(), anyString())).thenReturn(true);

        var record = MapRecord.create("stream", Map.of(
                "id", "event-1",
                "createdAt", Instant.now().toString(),
                "type", EventType.INVENTORY_ITEM_CREATED.name(),
                "payload", "{\"storeId\":\"s\",\"productId\":\"p\"}"));

        assertThrows(IllegalArgumentException.class, () -> listener.onMessage(record));
    }

    @Test
    void happyPathInvokesHandler() {
        var payloadRef = new AtomicReference<InventoryItemCreateEvent>();
        var handler = EventHandler.ofPayload(EventType.INVENTORY_ITEM_CREATED, InventoryItemCreateEvent.class,
                payloadRef::set);
        var listener = new EventListener(java.util.List.of(handler), mapper, redis);
        when(valueOps.setIfAbsent(anyString(), anyString())).thenReturn(true);

        var payloadJson = "{\"storeId\":\"s\",\"productId\":\"p\"}";
        var record = MapRecord.create("stream", Map.of(
                "id", "event-1",
                "createdAt", Instant.now().toString(),
                "type", EventType.INVENTORY_ITEM_CREATED.name(),
                "payload", payloadJson));

        listener.onMessage(record);

        assertNotNull(payloadRef.get());
        assertEquals("s", payloadRef.get().storeId());
        assertEquals("p", payloadRef.get().productId());
    }
}
