package com.tobiasbrandy.meli.inventory.model;

public record InventoryItemUpdateEvent(String storeId, String productId, int quantity) {
}
