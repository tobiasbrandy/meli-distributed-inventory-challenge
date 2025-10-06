package com.tobiasbrandy.meli.inventory.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tobiasbrandy.meli.inventory.model.Event;
import com.tobiasbrandy.meli.inventory.model.EventType;
import com.tobiasbrandy.meli.inventory.model.OutboxEvent;
import com.tobiasbrandy.meli.inventory.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher implements EventPublisher {
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final StringRedisTemplate redis;
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    @Override
    public <T> Event<T> publishEvent(final String stream, final EventType type, final T payload) {
        if (payload.getClass() != type.getPayloadType()) {
            throw new IllegalArgumentException("Payload of event " + type.name() + " must be of type " + type.getPayloadType().getSimpleName());
        }

        final String deserializedPayload;
        try {
            deserializedPayload = objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize event payload", e);
        }

        val outboxEvent = outboxEventRepository.save(new OutboxEvent(stream, type, deserializedPayload));

        val event = new Event<>(stream, outboxEvent.getEventId(), outboxEvent.getCreatedAt(), type, payload);
        log.info("Published event {}", event);
        return event;
    }

    @Scheduled(fixedDelay = 200)
    @Transactional
    protected void publish() {
        val events = outboxEventRepository.findByPublishedFalseOrderByIdAsc(PageRequest.ofSize(10));
        final List<Long> publishedEventIds = new ArrayList<>(events.size());

        if (disconnected.get()) return;

        try {
            for (val event : events) {
                redis.opsForStream().add(event.getStream(), Map.of(
                    "id", event.getEventId(),
                    "createdAt", event.getCreatedAt().toString(),
                    "type", event.getType().name(),
                    "payload", event.getPayload()
                ));
                publishedEventIds.add(event.getId());
            }
        } catch (final Exception e) {
            log.error("Error publishing events to stream", e);
        }

        outboxEventRepository.markPublished(publishedEventIds);
    }

    public void setDisconnected(final boolean disconnected) {
        this.disconnected.set(disconnected);
    }
}
