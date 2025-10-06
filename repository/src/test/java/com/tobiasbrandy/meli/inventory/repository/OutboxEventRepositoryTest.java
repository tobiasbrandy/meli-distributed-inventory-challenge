package com.tobiasbrandy.meli.inventory.repository;

import com.tobiasbrandy.meli.inventory.model.EventType;
import com.tobiasbrandy.meli.inventory.model.OutboxEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@org.springframework.test.context.ContextConfiguration(classes = RepositoryTestApplication.class)
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository repository;

    @Test
    void findByPublishedFalseOrderByIdAsc_returnsOrdered() {
        var e1 = repository.save(new OutboxEvent("s1", EventType.ECHO, "p1"));
        var e2 = repository.save(new OutboxEvent("s2", EventType.ECHO, "p2"));
        var page = repository.findByPublishedFalseOrderByIdAsc(PageRequest.ofSize(10));
        assertEquals(List.of(e1, e2), page);
    }

    @Test
    void markPublished_setsFlag() {
        var e1 = repository.save(new OutboxEvent("s1", EventType.ECHO, "p1"));
        var e2 = repository.save(new OutboxEvent("s2", EventType.ECHO, "p2"));

        int cnt = repository.markPublished(List.of(e1.getId(), e2.getId()));
        assertEquals(2, cnt);

        var remaining = repository.findByPublishedFalseOrderByIdAsc(PageRequest.ofSize(10));
        assertTrue(remaining.isEmpty());
    }
}
