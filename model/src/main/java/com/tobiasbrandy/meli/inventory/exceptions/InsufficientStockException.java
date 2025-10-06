package com.tobiasbrandy.meli.inventory.exceptions;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {
    private final String storeId;
    private final String productId;
    private final int currentStock;
    private final int requestedStock;
    public InsufficientStockException(final String storeId, final String productId, final int currentStock, final int requestedStock) {
        super(String.format("Insufficient stock for product %s in store %s: current %d, requested %d", productId, storeId, currentStock, requestedStock));
        this.storeId = storeId;
        this.productId = productId;
        this.currentStock = currentStock;
        this.requestedStock = requestedStock;
    }
}
