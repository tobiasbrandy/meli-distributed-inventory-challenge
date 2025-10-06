package com.tobiasbrandy.meli.inventory.store.api;

import com.tobiasbrandy.meli.inventory.messaging.EventPublisher;
import com.tobiasbrandy.meli.inventory.model.InventoryItem;
import com.tobiasbrandy.meli.inventory.store.config.AppConfig;
import com.tobiasbrandy.meli.inventory.store.service.HeartbeatService;
import com.tobiasbrandy.meli.inventory.store.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for store-local inventory operations and connectivity toggles.
 * <p>
 * Endpoints:
 * <ul>
 * <li>GET / — health check for the store server</li>
 * <li>POST /connected or /disconnected — simulate a store connectivity problem</li>
 * <li>GET /inventory — paginated list</li>
 * <li>GET /inventory/{productId} — fetch item for this store</li>
 * <li>POST /inventory — create item (quantity=0)</li>
 * <li>PUT /inventory/{productId} — set quantity</li>
 * <li>POST /purchase/{productId} — process a local purchase</li>
 * </ul>
 */
@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
public class InventoryController {
    public static final int MAX_PAGE_SIZE = 1000;
    public static final int DEFAULT_PAGE_SIZE = 20;

    private final AppConfig appConfig;
    private final InventoryService inventoryService;
    private final HeartbeatService heartbeatService;
    private final EventPublisher eventPublisher;

    @GetMapping("/")
    public String healthCheck() {
        return "Store Server " + appConfig.storeId() + ": OK";
    }

    @PostMapping("/connected")
    public String connected() {
        heartbeatService.setDisconnected(false);
        eventPublisher.setDisconnected(false);
        return "Connected";
    }

    @PostMapping("/disconnected")
    public String disconnected() {
        heartbeatService.setDisconnected(true);
        eventPublisher.setDisconnected(true);
        return "Disconnected";
    }

    @GetMapping("/inventory")
    public List<InventoryItem> listInventoryItem(
        @RequestParam(required = false) @Min(0) final Integer page,
        @RequestParam(required = false) @Min(0) @Max(MAX_PAGE_SIZE) final Integer size
    ) {
        return inventoryService.listInventoryItems(
            page == null ? 0 : page,
            size == null ? DEFAULT_PAGE_SIZE : size
        );
    }

    @GetMapping("/inventory/{productId}")
    public InventoryItem getInventoryItem(@PathVariable final String productId) {
        return inventoryService.getInventoryItem(productId);
    }

    public record CreateProductBody(@NotBlank String productId) {}
    @PostMapping("/inventory")
    public ResponseEntity<InventoryItem> createInventoryItem(@RequestBody @Valid final CreateProductBody body) {
        return new ResponseEntity<>(inventoryService.createInventoryItem(body.productId()), HttpStatus.CREATED);
    }

    public record SetInventoryItemQuantityBody(@Min(1) int quantity) {}
    @PutMapping("/inventory/{productId}")
    public ResponseEntity<?> setInventoryItemQuantity(
        @PathVariable final String productId,
        @RequestBody @Valid final SetInventoryItemQuantityBody body
    ) {
        inventoryService.setInventoryItemQuantity(productId, body.quantity());
        return ResponseEntity.noContent().build();
    }

    public record PurchaseBody(@Min(1) int quantity) {}
    @PostMapping("/purchase/{productId}")
    public ResponseEntity<InventoryItem> purchase(
        @PathVariable final String productId,
        @RequestBody @Valid final PurchaseBody body
    ) {
        return ResponseEntity.ok(inventoryService.processPurchase(productId, body.quantity()));
    }
}
