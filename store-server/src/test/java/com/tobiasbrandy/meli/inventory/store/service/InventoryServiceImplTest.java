package com.tobiasbrandy.meli.inventory.store.service;

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
import com.tobiasbrandy.meli.inventory.store.service.impl.InventoryServiceImpl;
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
    private AppConfig appConfig;
    private InventoryRepository repository;
    private EventPublisher publisher;
    private EventStreams streams;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        appConfig = new AppConfig("store-1");
        repository = mock(InventoryRepository.class);
        publisher = mock(EventPublisher.class);
        streams = mock(EventStreams.class);
        when(streams.storeToCentral("store-1")).thenReturn("s:store-1");

        service = new InventoryServiceImpl(appConfig, repository, publisher, streams);
    }

    @Test
    void getInventoryItem_returns() {
        var item = new InventoryItem("store-1", "p1", 10);
        when(repository.findByStoreIdAndProductId("store-1", "p1")).thenReturn(Optional.of(item));
        assertSame(item, service.getInventoryItem("p1"));
    }

    @Test
    void getInventoryItem_throwsNotFound() {
        when(repository.findByStoreIdAndProductId("store-1", "p1")).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> service.getInventoryItem("p1"));
    }

    @Test
    void createInventoryItem_savesAndPublishesCreated() {
        var saved = new InventoryItem("store-1", "p1", 0);
        when(repository.save(any(InventoryItem.class))).thenReturn(saved);

        var result = service.createInventoryItem("p1");
        assertSame(saved, result);

        ArgumentCaptor<InventoryItemCreateEvent> payload = ArgumentCaptor.forClass(InventoryItemCreateEvent.class);
        verify(publisher).publishEvent(eq("s:store-1"), eq(EventType.INVENTORY_ITEM_CREATED), payload.capture());
        assertEquals("store-1", payload.getValue().storeId());
        assertEquals("p1", payload.getValue().productId());
    }

    @Test
    void createInventoryItem_duplicateThrows() {
        when(repository.save(any(InventoryItem.class))).thenThrow(new DataIntegrityViolationException("dup"));
        assertThrows(ProductAlreadyExistsException.class, () -> service.createInventoryItem("p1"));
    }

    @Test
    void setInventoryItemQuantity_updatesAndPublishesUpdated() {
        when(repository.updateQuantity("store-1", "p1", 7)).thenReturn(1);
        service.setInventoryItemQuantity("p1", 7);

        ArgumentCaptor<InventoryItemUpdateEvent> payload = ArgumentCaptor.forClass(InventoryItemUpdateEvent.class);
        verify(publisher).publishEvent(eq("s:store-1"), eq(EventType.INVENTORY_ITEM_UPDATED), payload.capture());
        assertEquals("store-1", payload.getValue().storeId());
        assertEquals("p1", payload.getValue().productId());
        assertEquals(7, payload.getValue().quantity());
    }

    @Test
    void setInventoryItemQuantity_missingThrows() {
        when(repository.updateQuantity("store-1", "p1", 7)).thenReturn(0);
        assertThrows(ProductNotFoundException.class, () -> service.setInventoryItemQuantity("p1", 7));
    }

    @Test
    void processPurchase_happyPathDecrementsAndPublishesUpdated() {
        var item = new InventoryItem("store-1", "p1", 10);
        when(repository.findByStoreIdAndProductId("store-1", "p1")).thenReturn(Optional.of(item));
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        var saved = service.processPurchase("p1", 3);
        assertEquals(7, saved.getQuantity());

        ArgumentCaptor<InventoryItemUpdateEvent> payload = ArgumentCaptor.forClass(InventoryItemUpdateEvent.class);
        verify(publisher).publishEvent(eq("s:store-1"), eq(EventType.INVENTORY_ITEM_UPDATED), payload.capture());
        assertEquals(7, payload.getValue().quantity());
    }

    @Test
    void processPurchase_insufficientThrows() {
        var item = new InventoryItem("store-1", "p1", 2);
        when(repository.findByStoreIdAndProductId("store-1", "p1")).thenReturn(Optional.of(item));
        assertThrows(InsufficientStockException.class, () -> service.processPurchase("p1", 3));
    }

    @Test
    void processPurchase_notFoundThrows() {
        when(repository.findByStoreIdAndProductId("store-1", "p1")).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> service.processPurchase("p1", 1));
    }

    @Test
    void listInventoryItems_delegatesToRepository() {
        when(repository.findAll(PageRequest.of(0, 20)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        var list = service.listInventoryItems(0, 20);
        assertNotNull(list);
    }
}
