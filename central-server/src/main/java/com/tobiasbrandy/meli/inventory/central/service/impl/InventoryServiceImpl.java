package com.tobiasbrandy.meli.inventory.central.service.impl;

import com.tobiasbrandy.meli.inventory.central.service.HeartbeatService;
import com.tobiasbrandy.meli.inventory.central.service.InventoryService;
import com.tobiasbrandy.meli.inventory.exceptions.InsufficientStockException;
import com.tobiasbrandy.meli.inventory.exceptions.ProductAlreadyExistsException;
import com.tobiasbrandy.meli.inventory.exceptions.ProductNotFoundException;
import com.tobiasbrandy.meli.inventory.exceptions.StoreUnavailableException;
import com.tobiasbrandy.meli.inventory.messaging.EventPublisher;
import com.tobiasbrandy.meli.inventory.messaging.EventStreams;
import com.tobiasbrandy.meli.inventory.model.EventType;
import com.tobiasbrandy.meli.inventory.model.InventoryItem;
import com.tobiasbrandy.meli.inventory.model.InventoryItemRemotePurchaseEvent;
import com.tobiasbrandy.meli.inventory.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final HeartbeatService heartbeatService;
    private final EventPublisher eventPublisher;
    private final EventStreams eventStreams;

    @Override
    public InventoryItem getInventoryItem(final String storeId, final String productId) throws ProductNotFoundException {
        return inventoryRepository.findByStoreIdAndProductId(storeId, productId)
            .orElseThrow(() -> new ProductNotFoundException(storeId, productId));
    }

    @Override
    @Transactional
    public InventoryItem createInventoryItem(final String storeId, final String productId) throws ProductAlreadyExistsException {
        log.info("Creating product {}", productId);
        try {
            return inventoryRepository.save(new InventoryItem(storeId, productId, 0));
        } catch (DataIntegrityViolationException e) {
            log.error("Error creating product {}", productId, e);
            throw new ProductAlreadyExistsException(storeId, productId);
        }
    }

    @Override
    @Transactional
    public void setInventoryItemQuantity(final String storeId, final String productId, final int quantity) throws ProductNotFoundException {
        if (inventoryRepository.updateQuantity(storeId, productId, quantity) == 0) {
            throw new ProductNotFoundException(storeId, productId);
        }
    }

    @Override
    @Transactional
    public InventoryItem processPurchase(final String storeId, final String productId, final int quantity) throws ProductNotFoundException, InsufficientStockException {
        if (!heartbeatService.isAlive(storeId)) {
            throw new StoreUnavailableException(storeId);
        }

        val item = inventoryRepository.findByStoreIdAndProductId(storeId, productId)
            .orElseThrow(() -> new ProductNotFoundException(storeId, productId));

        if (item.getQuantity() < quantity) {
            throw new InsufficientStockException(storeId, productId, item.getQuantity(), quantity);
        }

        item.setQuantity(item.getQuantity() - quantity);
        val savedItem = inventoryRepository.save(item);

        eventPublisher.publishEvent(
            eventStreams.centralToStore(storeId),
            EventType.INVENTORY_ITEM_REMOTE_PURCHASE,
            new InventoryItemRemotePurchaseEvent(storeId, productId, quantity)
        );

        return savedItem;
    }

    @Override
    public List<InventoryItem> listInventoryItems(final int page, final int size) {
        return inventoryRepository.findAll(PageRequest.of(page, size)).toList();
    }
}
