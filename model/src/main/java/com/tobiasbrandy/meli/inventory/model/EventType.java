package com.tobiasbrandy.meli.inventory.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum EventType {
    ECHO(String.class),
    INVENTORY_ITEM_CREATED(InventoryItemCreateEvent.class),
    INVENTORY_ITEM_UPDATED(InventoryItemUpdateEvent.class),
    INVENTORY_ITEM_REMOTE_PURCHASE(InventoryItemRemotePurchaseEvent.class),
    ;

    private final Class<?> payloadType;
}
