package com.tobiasbrandy.meli.inventory.repository;

import com.tobiasbrandy.meli.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByStoreIdAndProductId(String storeId, String productId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE InventoryItem i
        SET i.quantity = :qty
        WHERE i.storeId = :storeId
          AND i.productId = :productId
    """)
    int updateQuantity(
        @Param("storeId") String storeId,
        @Param("productId") String productId,
        @Param("qty") int qty
    );
}
