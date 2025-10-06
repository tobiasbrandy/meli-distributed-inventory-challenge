package com.tobiasbrandy.meli.inventory.exceptions;

import lombok.Getter;

@Getter
public class ProductAlreadyExistsException extends RuntimeException {
    private final String storeId;
    private final String productId;
    public ProductAlreadyExistsException(final String storeId, final String productId) {
        super(String.format("Product %s already exists on store %s", productId, storeId));
        this.storeId = storeId;
        this.productId = productId;
    }
}
