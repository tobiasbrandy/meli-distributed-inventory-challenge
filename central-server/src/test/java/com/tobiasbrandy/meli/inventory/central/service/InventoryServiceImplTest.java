package com.tobiasbrandy.meli.inventory.central.service;

import com.tobiasbrandy.meli.inventory.central.service.impl.InventoryServiceImpl;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventoryServiceImplTest {
    private InventoryRepository repository;
    private HeartbeatService heartbeatService;
    private EventPublisher publisher;
    private EventStreams streams;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(InventoryRepository.class);
        heartbeatService = mock(HeartbeatService.class);
        publisher = mock(EventPublisher.class);
        streams = mock(EventStreams.class);
        when(streams.centralToStore("store-1")).thenReturn("centralTo:store-1");

        service = new InventoryServiceImpl(repository, heartbeatService, publisher, streams);
    }

    @Test
    void getInventoryItem_returns() {
        var item = new InventoryItem("store-1", "p1", 10);
        when(repository.findByStoreIdAndProductId("store-1", "p1")).thenReturn(Optional.of(item));
        assertSame(item, service.getInventoryItem("store-1", "p1"));
    }

    @Test
    void getInventoryItem_throwsNotFound() {
        when(repository.findByStoreIdAndProductId("store-1", "p1")).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> service.getInventoryItem("store-1", "p1"));
    }

    @Test
    void createInventoryItem_saves() {
        var saved = new InventoryItem("store-1", "p1", 0);
        when(repository.save(any(InventoryItem.class))).thenReturn(saved);
        assertSame(saved, service.createInventoryItem("store-1", "p1"));
    }

    @Test
    void createInventoryItem_duplicateThrows() {
        when(repository.save(any(InventoryItem.class))).thenThrow(new DataIntegrityViolationException("dup"));
        assertThrows(ProductAlreadyExistsException.class, () -> service.createInventoryItem("store-1", "p1"));
    }

    @Test
    void setInventoryItemQuantity_updatesOrThrows() {
        when(repository.updateQuantity("store-1", "p1", 7)).thenReturn(1);
        service.setInventoryItemQuantity("store-1", "p1", 7);
        when(repository.updateQuantity("store-1", "p1", 7)).thenReturn(0);
        assertThrows(ProductNotFoundException.class, () -> service.setInventoryItemQuantity("store-1", "p1", 7));
    }

    @Test
    void processPurchase_unavailableStoreThrows() {
        when(heartbeatService.isAlive("store-1")).thenReturn(false);
        assertThrows(StoreUnavailableException.class, () -> service.processPurchase("store-1", "p1", 1));
    }

    @Test
    void processPurchase_notFoundThrows() {
        when(heartbeatService.isAlive("store-1")).thenReturn(true);
        when(repository.findByStoreIdAndProductId("store-1", "p1")).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> service.processPurchase("store-1", "p1", 1));
    }

    @Test
    void processPurchase_insufficientThrows() {
        when(heartbeatService.isAlive("store-1")).thenReturn(true);
        var item = new InventoryItem("store-1", "p1", 1);
        when(repository.findByStoreIdAndProductId("store-1", "p1")).thenReturn(Optional.of(item));
        assertThrows(InsufficientStockException.class, () -> service.processPurchase("store-1", "p1", 2));
    }

    @Test
    void processPurchase_successDecrementsAndPublishesRemote() {
        when(heartbeatService.isAlive("store-1")).thenReturn(true);
        var item = new InventoryItem("store-1", "p1", 5);
        when(repository.findByStoreIdAndProductId("store-1", "p1")).thenReturn(Optional.of(item));
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.processPurchase("store-1", "p1", 3);
        assertEquals(2, saved.getQuantity());

        ArgumentCaptor<InventoryItemRemotePurchaseEvent> payload = ArgumentCaptor
                .forClass(InventoryItemRemotePurchaseEvent.class);
        verify(publisher).publishEvent(eq("centralTo:store-1"), eq(EventType.INVENTORY_ITEM_REMOTE_PURCHASE),
                payload.capture());
        assertEquals("p1", payload.getValue().productId());
        assertEquals(3, payload.getValue().quantityDelta());
    }

    @Test
    void listInventoryItems_delegates() {
        when(repository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        assertNotNull(service.listInventoryItems(0, 10));
    }
}
