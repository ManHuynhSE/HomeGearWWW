package iuh.fit.demoprj.tracking.service;

import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.OrderItemRepository;
import iuh.fit.demoprj.repository.ProductRepository;
import iuh.fit.demoprj.repository.UserAccountRepository;
import iuh.fit.demoprj.tracking.config.TrackingProperties;
import iuh.fit.demoprj.tracking.model.EngagementMetric;
import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.model.ProductEngagement;
import iuh.fit.demoprj.tracking.model.TrackingFunnel;
import iuh.fit.demoprj.tracking.repository.ProductEventRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {
    private static final int MAX_RESULT_LIMIT = 100;

    private final ProductEventRepository events;
    private final ProductRepository products;
    private final OrderItemRepository orderItems;
    private final UserAccountRepository accounts;
    private final TrackingProperties properties;
    private final Clock clock;

    public AnalyticsService(ProductEventRepository events, ProductRepository products,
                            OrderItemRepository orderItems, UserAccountRepository accounts,
                            TrackingProperties properties, Clock trackingClock) {
        this.events = events;
        this.products = products;
        this.orderItems = orderItems;
        this.accounts = accounts;
        this.properties = properties;
        this.clock = trackingClock;
    }

    public List<ProductEngagement> getTopProducts(
            LocalDateTime from, LocalDateTime to, int limit) {
        return getTopProducts(from, to, limit, EngagementMetric.SCORE);
    }

    public List<ProductEngagement> getTopProducts(
            LocalDateTime from, LocalDateTime to, int limit, EngagementMetric metric) {
        validateRange(from, to);
        validateLimit(limit);
        Comparator<ProductEngagement> comparator = comparatorFor(metric);
        return getAllEngagements(from, to).stream()
                .sorted(comparator)
                .limit(limit)
                .toList();
    }

    public java.util.Optional<ProductEngagement> getProductEngagement(
            Long productId, LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product ID must be positive.");
        }
        Product product = products.findById(productId).orElse(null);
        if (product == null) {
            return java.util.Optional.empty();
        }
        Map<Long, EventCounts> eventCounts = readEventCounts(from, to);
        Map<Long, Long> purchases = readPurchases(from, to);
        return java.util.Optional.of(toEngagement(product, eventCounts.get(productId),
                purchases.getOrDefault(productId, 0L)));
    }

    public List<ProductEngagement> getProductEngagement(
            String productName, LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank.");
        }
        List<Product> matches = products.findByNameContainingIgnoreCase(
                productName.trim(), PageRequest.of(0, MAX_RESULT_LIMIT));
        if (matches.isEmpty()) {
            return List.of();
        }
        Map<Long, EventCounts> eventCounts = readEventCounts(from, to);
        Map<Long, Long> purchases = readPurchases(from, to);
        return matches.stream()
                .map(product -> toEngagement(product, eventCounts.get(product.getId()),
                        purchases.getOrDefault(product.getId(), 0L)))
                .sorted(comparatorFor(EngagementMetric.SCORE))
                .toList();
    }

    public TrackingFunnel getFunnel(LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        List<ProductEngagement> engagement = getAllEngagements(from, to);
        long views = engagement.stream().mapToLong(ProductEngagement::views).sum();
        long addToCart = engagement.stream().mapToLong(ProductEngagement::addToCart).sum();
        long purchased = readPurchases(from, to).values().stream().mapToLong(Long::longValue).sum();
        return new TrackingFunnel(views, addToCart, purchased);
    }

    public List<ProductEngagement> getLowConversionProducts(
            LocalDateTime from, LocalDateTime to, long minViews, int limit) {
        validateRange(from, to);
        validateLimit(limit);
        if (minViews < 1) {
            throw new IllegalArgumentException("Minimum views must be positive.");
        }
        return getAllEngagements(from, to).stream()
                .filter(item -> item.views() >= minViews)
                .sorted(Comparator.comparing(ProductEngagement::conversionRate)
                        .thenComparing(Comparator.comparingLong(ProductEngagement::views).reversed())
                        .thenComparing(ProductEngagement::productName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ProductEngagement::productId))
                .limit(limit)
                .toList();
    }

    public List<ProductEngagement> getTrendingProducts(int limit) {
        validateLimit(limit);
        LocalDateTime to = LocalDateTime.now(clock);
        LocalDateTime from = to.minusDays(7);
        List<ProductEngagement> engagement = getAllEngagements(from, to);
        if (engagement.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = engagement.stream().map(ProductEngagement::productId).toList();
        Set<Long> availableIds = new HashSet<>();
        products.findByIdInAndActiveTrueAndStockGreaterThan(productIds, 0)
                .forEach(product -> availableIds.add(product.getId()));
        return engagement.stream()
                .filter(item -> availableIds.contains(item.productId()))
                .sorted(comparatorFor(EngagementMetric.SCORE))
                .limit(limit)
                .toList();
    }

    public List<Product> getRecentlyViewed(int limit) {
        validateLimit(limit);
        Long userId = currentCustomerId();
        if (userId == null) {
            return List.of();
        }
        List<Long> recentIds = events.findRecentProductViewsByUserId(
                        userId, EventType.PRODUCT_VIEW, PageRequest.of(0, limit))
                .stream()
                .map(ProductEventRepository.RecentProductViewProjection::getProductId)
                .toList();
        if (recentIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> activeProducts = new HashMap<>();
        products.findByIdInAndActiveTrue(recentIds)
                .forEach(product -> activeProducts.put(product.getId(), product));
        return recentIds.stream()
                .map(activeProducts::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<ProductEngagement> getAllEngagements(LocalDateTime from, LocalDateTime to) {
        Map<Long, EventCounts> eventCounts = readEventCounts(from, to);
        if (eventCounts.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> purchases = readPurchases(from, to);
        List<Product> existingProducts = new ArrayList<>();
        products.findAllById(eventCounts.keySet()).forEach(existingProducts::add);
        return existingProducts.stream()
                .map(product -> toEngagement(product, eventCounts.get(product.getId()),
                        purchases.getOrDefault(product.getId(), 0L)))
                .toList();
    }

    private Map<Long, EventCounts> readEventCounts(LocalDateTime from, LocalDateTime to) {
        Map<Long, EventCounts> counts = new HashMap<>();
        for (ProductEventRepository.EventCountProjection row
                : events.countByProductAndEventTypeBetween(from, to)) {
            EventCounts productCounts = counts.computeIfAbsent(row.getProductId(), ignored -> new EventCounts());
            switch (row.getEventType()) {
                case PRODUCT_VIEW -> productCounts.views = row.getEventCount();
                case PRODUCT_CLICK -> productCounts.clicks = row.getEventCount();
                case ADD_TO_CART -> productCounts.addToCart = row.getEventCount();
            }
        }
        return counts;
    }

    private Map<Long, Long> readPurchases(LocalDateTime from, LocalDateTime to) {
        Map<Long, Long> purchases = new HashMap<>();
        orderItems.sumPurchasedQuantityBetween(from, to)
                .forEach(row -> purchases.put(row.getProductId(), row.getPurchasedQuantity()));
        return purchases;
    }

    private ProductEngagement toEngagement(Product product, EventCounts counts, long purchasedQuantity) {
        EventCounts safeCounts = counts == null ? new EventCounts() : counts;
        long score = Math.addExact(
                Math.multiplyExact(safeCounts.clicks, properties.getWeight().getClick()),
                Math.multiplyExact(safeCounts.addToCart, properties.getWeight().getAddToCart()));
        BigDecimal conversionRate = safeCounts.views == 0 ? null
                : BigDecimal.valueOf(purchasedQuantity).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(safeCounts.views), 2, RoundingMode.HALF_UP);
        return new ProductEngagement(product.getId(), product.getName(), product.getImageUrl(),
                safeCounts.views, safeCounts.clicks, safeCounts.addToCart, score,
                purchasedQuantity, conversionRate);
    }

    private Long currentCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !hasCustomerRole(authentication)) {
            return null;
        }
        return accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(account -> account.getRole() == Role.CUSTOMER)
                .map(account -> account.getId())
                .orElse(null);
    }

    private boolean hasCustomerRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CUSTOMER"::equals);
    }

    private Comparator<ProductEngagement> comparatorFor(EngagementMetric metric) {
        EngagementMetric selectedMetric = metric == null ? EngagementMetric.SCORE : metric;
        Comparator<ProductEngagement> byMetric = switch (selectedMetric) {
            case SCORE -> Comparator.comparingLong(ProductEngagement::score);
            case CLICKS -> Comparator.comparingLong(ProductEngagement::clicks);
            case ADD_TO_CART -> Comparator.comparingLong(ProductEngagement::addToCart);
            case VIEWS -> Comparator.comparingLong(ProductEngagement::views);
        };
        return byMetric.reversed()
                .thenComparing(ProductEngagement::productName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ProductEngagement::productId);
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("Analytics range must satisfy from < to.");
        }
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_RESULT_LIMIT) {
            throw new IllegalArgumentException("Result limit must be between 1 and 100.");
        }
    }

    private static final class EventCounts {
        private long views;
        private long clicks;
        private long addToCart;
    }
}
