package com.tobiasbrandy.meli.inventory.central.service;

import com.tobiasbrandy.meli.inventory.exceptions.InsufficientStockException;
import com.tobiasbrandy.meli.inventory.exceptions.ProductAlreadyExistsException;
import com.tobiasbrandy.meli.inventory.exceptions.ProductNotFoundException;
import com.tobiasbrandy.meli.inventory.model.InventoryItem;

import java.util.List;

public interface InventoryService {

    InventoryItem getInventoryItem(String storeId, String productId) throws ProductNotFoundException;

    InventoryItem createInventoryItem(String storeId, String productId) throws ProductAlreadyExistsException;

    void setInventoryItemQuantity(String storeId, String productId, int quantity) throws ProductNotFoundException;

    List<InventoryItem> listInventoryItems(int page, int size);

    InventoryItem processPurchase(String storeId, String productId, final int quantity) throws ProductNotFoundException, InsufficientStockException;
}
