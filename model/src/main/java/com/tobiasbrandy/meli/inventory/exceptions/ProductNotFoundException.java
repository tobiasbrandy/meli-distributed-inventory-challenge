package com.tobiasbrandy.meli.inventory.exceptions;

import lombok.Getter;

@Getter
public class ProductNotFoundException extends RuntimeException {
    private final String storeId;
    private final String productId;
    public ProductNotFoundException(final String storeId, final String productId) {
        super(String.format("Product %s not found on store %s", productId, storeId));
        this.storeId = storeId;
        this.productId = productId;
    }
}
