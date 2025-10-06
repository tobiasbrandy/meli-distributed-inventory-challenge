package com.tobiasbrandy.meli.inventory.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tobiasbrandy.meli.inventory.model.Event;
import com.tobiasbrandy.meli.inventory.model.EventType;
import com.tobiasbrandy.meli.inventory.model.InventoryItemCreateEvent;
import com.tobiasbrandy.meli.inventory.model.InventoryItemUpdateEvent;
import com.tobiasbrandy.meli.inventory.model.OutboxEvent;
import com.tobiasbrandy.meli.inventory.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxEventPublisherTest {

    private ObjectMapper objectMapper;
    private OutboxEventRepository outboxRepo;
    private StringRedisTemplate redis;
    private StreamOperations<String, String, String> streamOps;
    private OutboxEventPublisher publisher;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        objectMapper = mock(ObjectMapper.class);
        outboxRepo = mock(OutboxEventRepository.class);
        redis = mock(StringRedisTemplate.class);
        streamOps = mock(StreamOperations.class);
        when(redis.opsForStream()).thenReturn((StreamOperations) streamOps);
        publisher = new OutboxEventPublisher(objectMapper, outboxRepo, redis);
    }

    @Test
    void publishEvent_rejectsMismatchedPayloadType() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publishEvent("s", EventType.INVENTORY_ITEM_CREATED,
                new InventoryItemUpdateEvent("s", "p", 1)));
    }

    @Test
    void publishEvent_throwsWhenSerializationFails() throws Exception {
        final var payload = new InventoryItemCreateEvent("store-1", "p1");
        when(objectMapper.writeValueAsString(payload)).thenThrow(new JsonProcessingException("fail") {
        });

        assertThrows(IllegalArgumentException.class,
                () -> publisher.publishEvent("stream", EventType.INVENTORY_ITEM_CREATED, payload));
    }

    @Test
    void publishEvent_persistsOutboxAndReturnsEvent() throws Exception {
        final var payload = new InventoryItemCreateEvent("store-1", "p1");
        when(objectMapper.writeValueAsString(payload)).thenReturn("{json}");

        final var saved = new OutboxEvent();
        saved.setId(10L);
        saved.setEventId("uuid-1");
        saved.setStream("stream");
        saved.setType(EventType.INVENTORY_ITEM_CREATED);
        saved.setPayload("{json}");
        saved.setCreatedAt(Instant.now());

        when(outboxRepo.save(any(OutboxEvent.class))).thenReturn(saved);

        Event<InventoryItemCreateEvent> event = publisher.publishEvent("stream", EventType.INVENTORY_ITEM_CREATED,
                payload);

        assertEquals("stream", event.stream());
        assertEquals("uuid-1", event.id());
        assertEquals(EventType.INVENTORY_ITEM_CREATED, event.type());
        assertEquals(payload, event.payload());
        assertNotNull(event.createdAt());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertEquals("stream", captor.getValue().getStream());
        assertEquals(EventType.INVENTORY_ITEM_CREATED, captor.getValue().getType());
        assertEquals("{json}", captor.getValue().getPayload());
    }

    @Test
    void scheduledPublish_noopWhenDisconnected() {
        publisher.setDisconnected(true);
        when(outboxRepo.findByPublishedFalseOrderByIdAsc(PageRequest.ofSize(10))).thenReturn(List.of(
                new OutboxEvent("s", EventType.ECHO, "p")));

        // call
        // noinspection CallToProtectedMethod
        publisher.publish();

        verify(redis, never()).opsForStream();
        verify(outboxRepo, never()).markPublished(anyList());
    }

    @Test
    void scheduledPublish_happyPathPublishesAndMarks() {
        final var a = new OutboxEvent("s1", EventType.ECHO, "p1");
        a.setId(1L);
        a.setEventId("id-1");
        a.setCreatedAt(Instant.now());
        final var b = new OutboxEvent("s2", EventType.ECHO, "p2");
        b.setId(2L);
        b.setEventId("id-2");
        b.setCreatedAt(Instant.now());
        when(outboxRepo.findByPublishedFalseOrderByIdAsc(PageRequest.ofSize(10))).thenReturn(List.of(a, b));

        // noinspection unchecked
        when(redis.opsForStream()).thenReturn((StreamOperations) streamOps);

        // call
        // noinspection CallToProtectedMethod
        publisher.publish();

        verify(streamOps, times(2)).add(anyString(), any(Map.class));
        verify(outboxRepo).markPublished(List.of(1L, 2L));
    }

    @Test
    void scheduledPublish_onRedisFailureStillMarksProcessedOnes() {
        final var a = new OutboxEvent("s1", EventType.ECHO, "p1");
        a.setId(1L);
        a.setEventId("id-1");
        a.setCreatedAt(Instant.now());
        when(outboxRepo.findByPublishedFalseOrderByIdAsc(PageRequest.ofSize(10))).thenReturn(List.of(a));

        // fail on add
        doThrow(new RuntimeException("boom")).when(streamOps).add(anyString(), any(Map.class));
        // noinspection unchecked
        when(redis.opsForStream()).thenReturn((StreamOperations) streamOps);

        // call
        // noinspection CallToProtectedMethod
        publisher.publish();

        // Since the failure happens before adding, no ids should be marked
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(outboxRepo).markPublished(idsCaptor.capture());
        assertTrue(idsCaptor.getValue().isEmpty());
    }
}
