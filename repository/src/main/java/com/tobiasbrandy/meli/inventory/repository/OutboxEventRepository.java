package com.tobiasbrandy.meli.inventory.repository;

import com.tobiasbrandy.meli.inventory.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findByPublishedFalseOrderByIdAsc(Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE OutboxEvent e SET e.published = true WHERE e.id IN :ids")
    int markPublished(@Param("ids") List<Long> ids);
}
