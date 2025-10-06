package com.tobiasbrandy.meli.inventory.store.service.impl;

import com.tobiasbrandy.meli.inventory.exceptions.InsufficientStockException;
import com.tobiasbrandy.meli.inventory.exceptions.ProductAlreadyExistsException;
import com.tobiasbrandy.meli.inventory.exceptions.ProductNotFoundException;
import com.tobiasbrandy.meli.inventory.messaging.EventPublisher;
import com.tobiasbrandy.meli.inventory.messaging.EventStreams;
import com.tobiasbrandy.meli.inventory.model.EventType;
import com.tobiasbrandy.meli.inventory.model.InventoryItem;
import com.tobiasbrandy.meli.inventory.model.InventoryItemCreateEvent;
import com.tobiasbrandy.meli.inventory.model.InventoryItemUpdateEvent;
import com.tobiasbrandy.meli.inventory.repository.InventoryRepository;
import com.tobiasbrandy.meli.inventory.store.config.AppConfig;
import com.tobiasbrandy.meli.inventory.store.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Central inventory service implementation.
 * <p>
 * Responsibilities:
 * <ul>
 * <li>CRUD operations on local store inventory</li>
 * <li>Signal central store of purchases</li>
 * <li>Emission of domain events via {@link EventPublisher}</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final AppConfig appConfig;
    private final InventoryRepository inventoryRepository;
    private final EventPublisher eventPublisher;
    private final EventStreams eventStreams;

    @Override
    public InventoryItem getInventoryItem(final String productId) throws ProductNotFoundException {
        return inventoryRepository.findByStoreIdAndProductId(appConfig.storeId(), productId)
                .orElseThrow(() -> new ProductNotFoundException(appConfig.storeId(), productId));
    }

    @Override
    @Transactional
    public InventoryItem createInventoryItem(final String productId) throws ProductAlreadyExistsException {
        log.info("Creating product {}", productId);
        try {
            val item = inventoryRepository.save(new InventoryItem(appConfig.storeId(), productId, 0));
            eventPublisher.publishEvent(
                eventStreams.storeToCentral(appConfig.storeId()),
                EventType.INVENTORY_ITEM_CREATED,
                new InventoryItemCreateEvent(item.getStoreId(), productId)
            );
            return item;
        } catch (DataIntegrityViolationException e) {
            log.error("Error creating product {}", productId, e);
            throw new ProductAlreadyExistsException(appConfig.storeId(), productId);
        }
    }

    @Override
    @Transactional
    public void setInventoryItemQuantity(final String productId, final int quantity) throws ProductNotFoundException {
        val storeId = appConfig.storeId();
        if (inventoryRepository.updateQuantity(storeId, productId, quantity) == 0) {
            throw new ProductNotFoundException(storeId, productId);
        }
        eventPublisher.publishEvent(
            eventStreams.storeToCentral(storeId),
            EventType.INVENTORY_ITEM_UPDATED,
            new InventoryItemUpdateEvent(storeId, productId, quantity)
        );
    }

    @Override
    @Transactional
    public InventoryItem processPurchase(final String productId, final int quantity) throws ProductNotFoundException, InsufficientStockException {
        val storeId = appConfig.storeId();
        val item = inventoryRepository.findByStoreIdAndProductId(storeId, productId)
            .orElseThrow(() -> new ProductNotFoundException(storeId, productId));

        if (item.getQuantity() < quantity) {
            throw new InsufficientStockException(storeId, productId, item.getQuantity(), quantity);
        }

        item.setQuantity(item.getQuantity() - quantity);
        val savedItem = inventoryRepository.save(item);

        eventPublisher.publishEvent(
            eventStreams.storeToCentral(storeId),
            EventType.INVENTORY_ITEM_UPDATED,
            new InventoryItemUpdateEvent(storeId, productId, savedItem.getQuantity())
        );

        log.info(
            "Processed local purchase: storeId={}, productId={}, quantityDelta={}, resultingQuantity={}",
            storeId, productId, quantity, savedItem.getQuantity()
        );

        return savedItem;
    }

    @Override
    public List<InventoryItem> listInventoryItems(final int page, final int size) {
        return inventoryRepository.findAll(PageRequest.of(page, size)).toList();
    }
}
