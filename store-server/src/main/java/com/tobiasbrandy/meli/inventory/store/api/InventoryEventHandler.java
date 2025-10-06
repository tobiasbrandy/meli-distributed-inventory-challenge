package com.tobiasbrandy.meli.inventory.store.api;

import com.tobiasbrandy.meli.inventory.messaging.EventHandler;
import com.tobiasbrandy.meli.inventory.model.EventType;
import com.tobiasbrandy.meli.inventory.model.InventoryItemRemotePurchaseEvent;
import com.tobiasbrandy.meli.inventory.store.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventHandler {
    private final InventoryService inventoryService;

    @Bean
    public EventHandler<InventoryItemRemotePurchaseEvent> remotePurchase() {
        return EventHandler.ofPayload(
            EventType.INVENTORY_ITEM_REMOTE_PURCHASE,
            InventoryItemRemotePurchaseEvent.class,
            event -> inventoryService.processPurchase(event.productId(), event.quantityDelta())
        );
    }
}
