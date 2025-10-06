package com.tobiasbrandy.meli.inventory.central.api;

import com.tobiasbrandy.meli.inventory.central.config.AppConfig;
import com.tobiasbrandy.meli.inventory.central.service.InventoryService;
import com.tobiasbrandy.meli.inventory.exceptions.InvalidStoreIdException;
import com.tobiasbrandy.meli.inventory.model.InventoryItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for central inventory operations.
 * <p>
 * Endpoints:
 * <ul>
 * <li>GET / — health check</li>
 * <li>GET /inventory — paginated list (defaults: page=0, size=20; maxsize=1000)</li>
 * <li>GET /inventory/{storeId}/{productId} — fetch item</li>
 * <li>POST /purchase/{storeId}/{productId} — process a remote purchase</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class InventoryController {
    public static final int MAX_PAGE_SIZE = 1000;
    public static final int DEFAULT_PAGE_SIZE = 20;

    private final AppConfig appConfig;
    private final InventoryService inventoryService;

    private String validateStoreId(final String storeId) {
        if (!appConfig.stores().contains(storeId)) {
            throw new InvalidStoreIdException(storeId);
        }
        return storeId;
    }

    @GetMapping("/")
    public String healthCheck() {
        return "Central Server: OK";
    }

    @GetMapping("/inventory")
    public List<InventoryItem> listInventoryItem(
        @RequestParam(required = false) final Integer page,
        @RequestParam(required = false) @Min(1) @Max(MAX_PAGE_SIZE) final Integer size
    ) {
        return inventoryService.listInventoryItems(
            page == null ? 0 : page,
            size == null ? DEFAULT_PAGE_SIZE : size
        );
    }

    @GetMapping("/inventory/{storeId}/{productId}")
    public InventoryItem getInventoryItem(@PathVariable final String storeId, @PathVariable final String productId) {
        return inventoryService.getInventoryItem(validateStoreId(storeId), productId);
    }

    public record PurchaseBody(@Min(1) int quantity) {}
    @PostMapping("/purchase/{storeId}/{productId}")
    public ResponseEntity<InventoryItem> purchase(
        @PathVariable final String storeId,
        @PathVariable final String productId,
        @RequestBody @Valid final PurchaseBody body
    ) {
        return ResponseEntity.ok(inventoryService.processPurchase(validateStoreId(storeId), productId, body.quantity()));
    }
}
