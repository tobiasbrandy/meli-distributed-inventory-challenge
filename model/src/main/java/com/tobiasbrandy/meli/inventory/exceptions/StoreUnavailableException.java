package com.tobiasbrandy.meli.inventory.exceptions;

import lombok.Getter;

@Getter
public class StoreUnavailableException extends RuntimeException {
    private final String storeId;
    public StoreUnavailableException(final String storeId) {
        super(String.format("Store %s is not reachable", storeId));
        this.storeId = storeId;
    }
}
