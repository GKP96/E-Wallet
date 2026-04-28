package com.example.jbdl.ewallet.entity;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String payload;

    @Column(nullable = false)
    public boolean published = false;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();
}
