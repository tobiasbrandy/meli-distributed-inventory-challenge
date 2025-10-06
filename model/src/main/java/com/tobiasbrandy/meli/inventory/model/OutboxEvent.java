package com.tobiasbrandy.meli.inventory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event", uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable=false, unique=true)
    String eventId; // UUID

    @Column(nullable=false)
    private String stream;

    @Column(nullable=false)
    private Instant createdAt;

    @Column(nullable=false)
    private EventType type;

    @Lob @Column(nullable=false)
    private String payload;

    @Column(nullable=false)
    private boolean published = false;

    public OutboxEvent(final String stream, final EventType type, final String payload) {
        this.eventId = UUID.randomUUID().toString();
        this.stream = stream;
        this.type = type;
        this.payload = payload;
        this.createdAt = Instant.now();
    }
}
