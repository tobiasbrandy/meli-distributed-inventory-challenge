package com.tobiasbrandy.meli.inventory.store.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tobiasbrandy.meli.inventory.messaging.EventPublisher;
import com.tobiasbrandy.meli.inventory.model.InventoryItem;
import com.tobiasbrandy.meli.inventory.store.config.AppConfig;
import com.tobiasbrandy.meli.inventory.store.service.HeartbeatService;
import com.tobiasbrandy.meli.inventory.store.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryControllerTest {
    private MockMvc mvc;
    private ObjectMapper mapper;
    private InventoryService inventoryService;
    private HeartbeatService heartbeatService;
    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        inventoryService = mock(InventoryService.class);
        heartbeatService = mock(HeartbeatService.class);
        eventPublisher = mock(EventPublisher.class);
        var controller = new InventoryController(new AppConfig("store-1"), inventoryService, heartbeatService,
                eventPublisher);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void health() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Store Server")));
    }

    @Test
    void listInventory_defaults() throws Exception {
        when(inventoryService.listInventoryItems(0, 20)).thenReturn(List.of());
        mvc.perform(get("/inventory"))
                .andExpect(status().isOk());
    }

    @Test
    void getInventoryItem_ok() throws Exception {
        when(inventoryService.getInventoryItem("p1")).thenReturn(new InventoryItem("store-1", "p1", 1));
        mvc.perform(get("/inventory/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is("p1")));
    }

    @Test
    void createInventoryItem_valid() throws Exception {
        when(inventoryService.createInventoryItem("p1")).thenReturn(new InventoryItem("store-1", "p1", 0));

        var body = mapper.writeValueAsString(new InventoryController.CreateProductBody("p1"));
        mvc.perform(post("/inventory").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId", is("p1")));
    }

    @Test
    void setInventoryItemQuantity_validatesAndCallsService() throws Exception {
        var body = mapper.writeValueAsString(new InventoryController.SetInventoryItemQuantityBody(5));
        mvc.perform(put("/inventory/p1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        verify(inventoryService).setInventoryItemQuantity("p1", 5);
    }

    @Test
    void purchase_validatesAndReturnsItem() throws Exception {
        when(inventoryService.processPurchase(eq("p1"), anyInt())).thenReturn(new InventoryItem("store-1", "p1", 5));
        var body = mapper.writeValueAsString(new InventoryController.PurchaseBody(1));
        mvc.perform(post("/purchase/p1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is("p1")));
    }

    @Test
    void connected_disconnected_endpoints() throws Exception {
        mvc.perform(post("/connected")).andExpect(status().isOk());
        mvc.perform(post("/disconnected")).andExpect(status().isOk());
        verify(heartbeatService).setDisconnected(false);
        verify(heartbeatService).setDisconnected(true);
        verify(eventPublisher).setDisconnected(false);
        verify(eventPublisher).setDisconnected(true);
    }
}
