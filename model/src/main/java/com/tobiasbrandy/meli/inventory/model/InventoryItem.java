package com.tobiasbrandy.meli.inventory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name="inventory_item",
    uniqueConstraints=@UniqueConstraint(columnNames={"productId", "storeId"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable=false)
    private String storeId;

    @Column(nullable=false)
    private String productId;

    @Column(nullable=false)
    private int quantity;

    public InventoryItem(String storeId, String productId, int quantity) {
        this.storeId = storeId;
        this.productId = productId;
        this.quantity = quantity;
    }
}
