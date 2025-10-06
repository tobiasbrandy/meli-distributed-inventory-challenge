package com.tobiasbrandy.meli.inventory.central.api;

import com.tobiasbrandy.meli.inventory.central.service.InventoryService;
import com.tobiasbrandy.meli.inventory.messaging.EventHandler;
import com.tobiasbrandy.meli.inventory.model.EventType;
import com.tobiasbrandy.meli.inventory.model.InventoryItemCreateEvent;
import com.tobiasbrandy.meli.inventory.model.InventoryItemUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * All central server inventory event handlers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventHandler {
    private final InventoryService inventoryService;

    @Bean
    public EventHandler<InventoryItemCreateEvent> itemCreated() {
        return EventHandler.ofPayload(
            EventType.INVENTORY_ITEM_CREATED,
            InventoryItemCreateEvent.class,
            event -> inventoryService.createInventoryItem(event.storeId(), event.productId())
        );
    }

    @Bean
    public EventHandler<InventoryItemUpdateEvent> itemUpdated() {
        return EventHandler.ofPayload(
            EventType.INVENTORY_ITEM_UPDATED,
            InventoryItemUpdateEvent.class,
            event -> inventoryService.setInventoryItemQuantity(event.storeId(), event.productId(), event.quantity())
        );
    }
}
