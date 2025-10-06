package com.tobiasbrandy.meli.inventory.store.api;

import com.tobiasbrandy.meli.inventory.model.InventoryItemRemotePurchaseEvent;
import com.tobiasbrandy.meli.inventory.store.service.InventoryService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InventoryEventHandlerTest {
    @Test
    void remotePurchaseHandlerInvokesService() {
        var service = mock(InventoryService.class);
        var handlerComp = new InventoryEventHandler(service);
        var handler = handlerComp.remotePurchase();

        handler.handleEvent(new com.tobiasbrandy.meli.inventory.model.Event<>(
                "s", "id", java.time.Instant.now(),
                com.tobiasbrandy.meli.inventory.model.EventType.INVENTORY_ITEM_REMOTE_PURCHASE,
                new InventoryItemRemotePurchaseEvent("store-1", "p1", 3)));

        verify(service).processPurchase(eq("p1"), eq(3));
    }
}
