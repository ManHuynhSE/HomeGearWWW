package iuh.fit.demoprj.tracking.model;

import java.math.BigDecimal;

public record ProductEngagement(
        Long productId,
        String productName,
        String imageUrl,
        long views,
        long clicks,
        long addToCart,
        long score,
        long purchasedQuantity,
        BigDecimal conversionRate) {
}
