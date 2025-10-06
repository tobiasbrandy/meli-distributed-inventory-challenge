package com.tobiasbrandy.meli.inventory.model;

public record InventoryItemRemotePurchaseEvent(String storeId, String productId, int quantityDelta) {
}
