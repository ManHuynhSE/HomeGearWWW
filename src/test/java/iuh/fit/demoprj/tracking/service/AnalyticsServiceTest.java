package iuh.fit.demoprj.tracking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.OrderItemRepository;
import iuh.fit.demoprj.repository.ProductRepository;
import iuh.fit.demoprj.repository.UserAccountRepository;
import iuh.fit.demoprj.tracking.config.TrackingProperties;
import iuh.fit.demoprj.tracking.model.EngagementMetric;
import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.model.ProductEngagement;
import iuh.fit.demoprj.tracking.repository.ProductEventRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 11, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 11, 9, 0, 0);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-09T05:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private ProductEventRepository events;
    @Mock
    private ProductRepository products;
    @Mock
    private OrderItemRepository orderItems;
    @Mock
    private UserAccountRepository accounts;

    private TrackingProperties properties;
    private AnalyticsService analytics;
    private Product alpha;
    private Product beta;

    @BeforeEach
    void setUp() {
        properties = new TrackingProperties();
        properties.getWeight().setClick(2);
        properties.getWeight().setAddToCart(5);
        analytics = new AnalyticsService(events, products, orderItems, accounts, properties, CLOCK);
        alpha = product(1L, "Alpha");
        beta = product(2L, "Beta");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ranksByConfiguredWeightsAndCalculatesConversionForTheSameWindow() {
        List<ProductEventRepository.EventCountProjection> eventCounts = List.of(
                eventCount(1L, EventType.PRODUCT_VIEW, 4),
                eventCount(1L, EventType.PRODUCT_CLICK, 2),
                eventCount(1L, EventType.ADD_TO_CART, 1),
                eventCount(2L, EventType.PRODUCT_VIEW, 10),
                eventCount(2L, EventType.PRODUCT_CLICK, 3));
        List<OrderItemRepository.ProductPurchaseProjection> purchases = List.of(
                purchaseCount(1L, 8), purchaseCount(2L, 0));
        when(events.countByProductAndEventTypeBetween(FROM, TO)).thenReturn(eventCounts);
        when(orderItems.sumPurchasedQuantityBetween(FROM, TO)).thenReturn(purchases);
        when(products.findAllById(any())).thenReturn(List.of(alpha, beta));

        List<ProductEngagement> topByScore = analytics.getTopProducts(FROM, TO, 2);
        List<ProductEngagement> topByClicks =
                analytics.getTopProducts(FROM, TO, 2, EngagementMetric.CLICKS);

        assertEquals(List.of(1L, 2L), topByScore.stream().map(ProductEngagement::productId).toList());
        assertEquals(9L, topByScore.get(0).score());
        assertEquals(new BigDecimal("200.00"), topByScore.get(0).conversionRate());
        assertEquals(List.of(2L, 1L), topByClicks.stream().map(ProductEngagement::productId).toList());

        properties.getWeight().setClick(4);
        properties.getWeight().setAddToCart(0);
        assertEquals(2L, analytics.getTopProducts(FROM, TO, 1).get(0).productId());
    }

    @Test
    void excludesOrphanedProductEventsAndUsesHalfOpenWindowForFunnel() {
        List<ProductEventRepository.EventCountProjection> eventCounts = List.of(
                eventCount(1L, EventType.PRODUCT_VIEW, 4),
                eventCount(1L, EventType.ADD_TO_CART, 1),
                eventCount(99L, EventType.PRODUCT_VIEW, 100));
        List<OrderItemRepository.ProductPurchaseProjection> purchases = List.of(purchaseCount(1L, 2));
        when(events.countByProductAndEventTypeBetween(FROM, TO)).thenReturn(eventCounts);
        when(orderItems.sumPurchasedQuantityBetween(FROM, TO)).thenReturn(purchases);
        when(products.findAllById(any())).thenReturn(List.of(alpha));

        var funnel = analytics.getFunnel(FROM, TO);

        assertEquals(4L, funnel.views());
        assertEquals(1L, funnel.addToCart());
        assertEquals(2L, funnel.purchasedQuantity());
        verify(events).countByProductAndEventTypeBetween(FROM, TO);
        verify(orderItems).sumPurchasedQuantityBetween(FROM, TO);
    }

    @Test
    void returnsNullConversionWhenProductHasNoViews() {
        when(products.findById(1L)).thenReturn(Optional.of(alpha));
        when(events.countByProductAndEventTypeBetween(FROM, TO)).thenReturn(List.of());
        List<OrderItemRepository.ProductPurchaseProjection> purchases = List.of(purchaseCount(1L, 3));
        when(orderItems.sumPurchasedQuantityBetween(FROM, TO)).thenReturn(purchases);

        ProductEngagement engagement = analytics.getProductEngagement(1L, FROM, TO).orElseThrow();

        assertEquals(3L, engagement.purchasedQuantity());
        assertNull(engagement.conversionRate());
    }

    @Test
    void onlyReadsRecentlyViewedProductsForTheAuthenticatedCustomer() {
        UserAccount customer = new UserAccount("Customer", "customer@example.com", "password", Role.CUSTOMER);
        ReflectionTestUtils.setField(customer, "id", 42L);
        when(accounts.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        List<ProductEventRepository.RecentProductViewProjection> recentViews =
                List.of(recentView(2L), recentView(1L));
        when(events.findRecentProductViewsByUserId(eq(42L), eq(EventType.PRODUCT_VIEW), any(Pageable.class)))
                .thenReturn(recentViews);
        when(products.findByIdInAndActiveTrue(List.of(2L, 1L))).thenReturn(List.of(alpha, beta));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("customer@example.com", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        List<Product> recentlyViewed = analytics.getRecentlyViewed(2);

        assertEquals(List.of(2L, 1L), recentlyViewed.stream().map(Product::getId).toList());
        verify(events).findRecentProductViewsByUserId(eq(42L), eq(EventType.PRODUCT_VIEW), any(Pageable.class));
    }

    @Test
    void ranksLowConversionProductsByConversionAndLimitsTrendingToAvailableProducts() {
        List<ProductEventRepository.EventCountProjection> eventCounts = List.of(
                eventCount(1L, EventType.PRODUCT_VIEW, 10),
                eventCount(2L, EventType.PRODUCT_VIEW, 5));
        List<OrderItemRepository.ProductPurchaseProjection> purchases = List.of(
                purchaseCount(1L, 0), purchaseCount(2L, 1));
        when(events.countByProductAndEventTypeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(eventCounts);
        when(orderItems.sumPurchasedQuantityBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(purchases);
        when(products.findAllById(any())).thenReturn(List.of(alpha, beta));
        when(products.findByIdInAndActiveTrueAndStockGreaterThan(List.of(1L, 2L), 0))
                .thenReturn(List.of(beta));

        List<ProductEngagement> lowConversion =
                analytics.getLowConversionProducts(FROM, TO, 5, 2);
        List<ProductEngagement> trending = analytics.getTrendingProducts(5);

        assertEquals(List.of(1L, 2L), lowConversion.stream().map(ProductEngagement::productId).toList());
        assertEquals(new BigDecimal("0.00"), lowConversion.get(0).conversionRate());
        assertEquals(List.of(2L), trending.stream().map(ProductEngagement::productId).toList());
        verify(events).countByProductAndEventTypeBetween(
                LocalDateTime.of(2026, 11, 2, 12, 0), LocalDateTime.of(2026, 11, 9, 12, 0));
    }

    @Test
    void doesNotReadRecentlyViewedProductsForGuests() {
        SecurityContextHolder.clearContext();

        assertEquals(List.of(), analytics.getRecentlyViewed(5));

        verify(accounts, never()).findByEmailIgnoreCase(any());
        verify(events, never()).findRecentProductViewsByUserId(any(), any(), any());
    }

    @Test
    void rejectsInvalidRangesAndLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> analytics.getTopProducts(TO, FROM, 5));
        assertThrows(IllegalArgumentException.class,
                () -> analytics.getTopProducts(FROM, TO, 0));
        assertThrows(IllegalArgumentException.class,
                () -> analytics.getRecentlyViewed(101));
    }

    private Product product(Long id, String name) {
        Product product = new Product(name, BigDecimal.TEN, 4, "", new Category("Category"));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private ProductEventRepository.EventCountProjection eventCount(
            Long productId, EventType eventType, long count) {
        ProductEventRepository.EventCountProjection projection =
                mock(ProductEventRepository.EventCountProjection.class);
        when(projection.getProductId()).thenReturn(productId);
        when(projection.getEventType()).thenReturn(eventType);
        when(projection.getEventCount()).thenReturn(count);
        return projection;
    }

    private OrderItemRepository.ProductPurchaseProjection purchaseCount(Long productId, long quantity) {
        OrderItemRepository.ProductPurchaseProjection projection =
                mock(OrderItemRepository.ProductPurchaseProjection.class);
        when(projection.getProductId()).thenReturn(productId);
        when(projection.getPurchasedQuantity()).thenReturn(quantity);
        return projection;
    }

    private ProductEventRepository.RecentProductViewProjection recentView(Long productId) {
        ProductEventRepository.RecentProductViewProjection projection =
                mock(ProductEventRepository.RecentProductViewProjection.class);
        when(projection.getProductId()).thenReturn(productId);
        return projection;
    }
}
