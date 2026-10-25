
package com.stockdesk.stockdesk;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StockMovement {

    private final int id;
    private final int productId;
    private final String product;
    private final String type;
    private final int quantity;
    private final LocalDateTime createdAt;

    public StockMovement(int id, int productId, String product,
                         String type, int quantity) {
        this.id = id;
        this.productId = productId;
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public int getProductId() {
        return productId;
    }

    public String getProduct() {
        return product;
    }

    public String getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getDate() {
        return createdAt.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
    }
}
