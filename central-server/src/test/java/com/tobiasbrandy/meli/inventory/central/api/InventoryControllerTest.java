package com.tobiasbrandy.meli.inventory.central.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tobiasbrandy.meli.inventory.central.config.AppConfig;
import com.tobiasbrandy.meli.inventory.central.config.GlobalExceptionHandler;
import com.tobiasbrandy.meli.inventory.central.service.InventoryService;
import com.tobiasbrandy.meli.inventory.exceptions.InvalidStoreIdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryControllerTest {

    private MockMvc mvc;
    private ObjectMapper mapper;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        inventoryService = mock(InventoryService.class);
        var controller = new InventoryController(new AppConfig(List.of("store-1")), inventoryService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void health() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void listInventory_defaults() throws Exception {
        when(inventoryService.listInventoryItems(0, 20)).thenReturn(List.of());
        mvc.perform(get("/inventory")).andExpect(status().isOk());
    }

    @Test
    void getInventoryItem_ok() throws Exception {
        when(inventoryService.getInventoryItem("store-1", "p1"))
                .thenReturn(new com.tobiasbrandy.meli.inventory.model.InventoryItem("store-1", "p1", 1));
        mvc.perform(get("/inventory/store-1/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is("p1")));
    }

    @Test
    void getInventoryItem_invalidStore() throws Exception {
        doThrow(new InvalidStoreIdException("bad-store")).when(inventoryService).getInventoryItem(anyString(), anyString());
        mvc.perform(get("/inventory/bad-store/p1")).andExpect(status().isBadRequest());
    }

    @Test
    void purchase_validatesAndReturnsItem() throws Exception {
        when(inventoryService.processPurchase(eq("store-1"), eq("p1"), anyInt()))
                .thenReturn(new com.tobiasbrandy.meli.inventory.model.InventoryItem("store-1", "p1", 5));
        var body = mapper.writeValueAsString(new InventoryController.PurchaseBody(1));
        mvc.perform(post("/purchase/store-1/p1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is("p1")));
    }

    @Test
    void purchase_invalidQuantity() throws Exception {
        var body = mapper.writeValueAsString(new InventoryController.PurchaseBody(0));
        mvc.perform(post("/purchase/store-1/p1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void methodNotAllowed_returnsProblem() throws Exception {
        mvc.perform(put("/inventory")).andExpect(status().isMethodNotAllowed());
    }
}
