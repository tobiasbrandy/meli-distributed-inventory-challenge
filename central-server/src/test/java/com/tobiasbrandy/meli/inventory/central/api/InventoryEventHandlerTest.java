package com.tobiasbrandy.meli.inventory.central.api;

import com.tobiasbrandy.meli.inventory.model.InventoryItemCreateEvent;
import com.tobiasbrandy.meli.inventory.model.InventoryItemUpdateEvent;
import com.tobiasbrandy.meli.inventory.central.service.InventoryService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InventoryEventHandlerTest {
    @Test
    void createdAndUpdatedHandlersInvokeService() {
        var service = mock(InventoryService.class);
        var handlerComp = new InventoryEventHandler(service);

        handlerComp.itemCreated().handleEvent(new com.tobiasbrandy.meli.inventory.model.Event<>(
                "s", "id", java.time.Instant.now(),
                com.tobiasbrandy.meli.inventory.model.EventType.INVENTORY_ITEM_CREATED,
                new InventoryItemCreateEvent("store-1", "p1")));
        verify(service).createInventoryItem("store-1", "p1");

        handlerComp.itemUpdated().handleEvent(new com.tobiasbrandy.meli.inventory.model.Event<>(
                "s", "id", java.time.Instant.now(),
                com.tobiasbrandy.meli.inventory.model.EventType.INVENTORY_ITEM_UPDATED,
                new InventoryItemUpdateEvent("store-1", "p1", 7)));
        verify(service).setInventoryItemQuantity(eq("store-1"), eq("p1"), eq(7));
    }
}
