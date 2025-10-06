package com.tobiasbrandy.meli.inventory.store.service;

import com.tobiasbrandy.meli.inventory.exceptions.InsufficientStockException;
import com.tobiasbrandy.meli.inventory.exceptions.ProductAlreadyExistsException;
import com.tobiasbrandy.meli.inventory.exceptions.ProductNotFoundException;
import com.tobiasbrandy.meli.inventory.model.InventoryItem;

import java.util.List;

public interface InventoryService {

    InventoryItem getInventoryItem(String productId) throws ProductNotFoundException;

    List<InventoryItem> listInventoryItems(int page, int size);

    InventoryItem createInventoryItem(String productId) throws ProductAlreadyExistsException;

    void setInventoryItemQuantity(String productId, int quantity) throws ProductNotFoundException;

    InventoryItem processPurchase(String productId, int quantity) throws ProductNotFoundException, InsufficientStockException;
}
