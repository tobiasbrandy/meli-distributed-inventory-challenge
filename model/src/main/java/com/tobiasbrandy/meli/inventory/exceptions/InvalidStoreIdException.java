package com.tobiasbrandy.meli.inventory.exceptions;

import lombok.Getter;

@Getter
public class InvalidStoreIdException extends RuntimeException {
    private final String storeId;
    public InvalidStoreIdException(final String storeId) {
        super(String.format("Store %s doesn't exist", storeId));
        this.storeId = storeId;
    }
}
