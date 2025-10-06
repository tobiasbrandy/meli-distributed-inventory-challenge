package com.tobiasbrandy.meli.inventory.repository;

import com.tobiasbrandy.meli.inventory.model.InventoryItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@org.springframework.test.context.ContextConfiguration(classes = RepositoryTestApplication.class)
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository repository;

    @Test
    void updateQuantity_updatesExistingRow() {
        var saved = repository.save(new InventoryItem("store-1", "p1", 10));

        int updated = repository.updateQuantity("store-1", "p1", 5);
        assertEquals(1, updated);

        var reloaded = repository.findByStoreIdAndProductId("store-1", "p1").orElseThrow();
        assertEquals(5, reloaded.getQuantity());
    }

    @Test
    void updateQuantity_returnsZeroWhenMissing() {
        int updated = repository.updateQuantity("store-1", "missing", 5);
        assertEquals(0, updated);
    }

    @Test
    void uniqueConstraintOnStoreIdAndProductId() {
        repository.save(new InventoryItem("store-1", "p1", 1));
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new InventoryItem("store-1", "p1", 2)));
    }
}
