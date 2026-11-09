package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.ai.tools.AdminTools;
import iuh.fit.demoprj.ai.tools.AiToolInvocationLogger;
import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.service.AdminService;
import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.tracking.model.EngagementMetric;
import iuh.fit.demoprj.tracking.model.ProductEngagement;
import iuh.fit.demoprj.tracking.model.TrackingFunnel;
import iuh.fit.demoprj.tracking.service.AnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminToolsTest {
    @Mock
    private CatalogService catalog;
    @Mock
    private AdminService admin;
    @Mock
    private AnalyticsService analytics;
    @Mock
    private AiToolInvocationLogger invocationLogger;

    private AdminTools tools;
    private Product product;
    private final LocalDate from = LocalDate.of(2026, 11, 1);
    private final LocalDate to = LocalDate.of(2026, 11, 7);

    @BeforeEach
    void setUp() {
        tools = new AdminTools(catalog, admin, analytics, invocationLogger);
        product = new Product("Quạt", BigDecimal.valueOf(250_000), 3,
                "Quạt gia đình", new Category("Điện tử"));
    }

    @Test
    void lowStockDefaultsToFiveAndReturnsOnlyStockSummary() {
        when(catalog.findLowStockProducts(5)).thenReturn(List.of(product));

        var result = tools.getLowStockProducts(null);

        assertEquals(3, result.data().get(0).stock());
        assertEquals("Quạt", result.data().get(0).name());
    }

    @Test
    void stockLookupUsesCatalogSearch() {
        when(catalog.searchProductsForAssistant("Quạt", null, null, null)).thenReturn(List.of(product));

        var result = tools.getStockByProduct("Quạt");

        assertEquals(3, result.data().get(0).stock());
    }

    @Test
    void categoryCountsAreReturnedWithoutCustomerData() {
        when(catalog.countProductsByCategory())
                .thenReturn(List.of(new CatalogService.CategoryProductCount("Điện tử", 4)));

        var result = tools.countProductsByCategory();

        assertEquals(4, result.data().get(0).productCount());
    }

    @Test
    void revenueToolRequiresValidDateRangeAndReturnsServiceValue() {
        when(admin.calculateRevenue(from, to)).thenReturn(BigDecimal.valueOf(900_000));

        var result = tools.getRevenue(from, to);

        assertEquals(BigDecimal.valueOf(900_000), result.data().completedRevenue());
        assertEquals(from, result.data().fromDate());
    }

    @Test
    void topProductsAreLimitedAndMappedToSafeSummary() {
        when(admin.getTopSellingProducts(from, to, 3))
                .thenReturn(List.of(new AdminService.ProductSales(product, 8)));

        var result = tools.getTopSellingProducts(from, to, 3);

        assertEquals(8, result.data().get(0).unitsSold());
        assertEquals("Quạt", result.data().get(0).product().name());
    }

    @Test
    void ordersSummaryUsesOnlyAggregatedAdminServiceResult() {
        var summary = new AdminService.OrdersSummary(from, to, 2, Map.of("COMPLETED", 2L),
                BigDecimal.valueOf(900_000), BigDecimal.valueOf(900_000));
        when(admin.getOrdersSummary(from, to)).thenReturn(summary);

        var result = tools.getOrdersSummary(from, to);

        assertEquals(2, result.data().totalOrders());
        assertEquals(BigDecimal.valueOf(900_000), result.data().completedRevenue());
    }

    @Test
    void missingAdminReportDatesAreRejectedBeforeServiceCall() {
        var result = tools.getRevenue(null, null);

        assertNull(result.data());
        assertEquals("Vui lòng cung cấp khoảng ngày ISO hợp lệ, ngày kết thúc không trước ngày bắt đầu.",
                result.message());
    }

    @Test
    void topViewedProductsUsesInclusiveIsoDatesAndDefaultMetricAndLimit() {
        ProductEngagement engagement = new ProductEngagement(
                12L, "Quạt", "/images/fan.jpg", 20, 4, 2, 10, 1, BigDecimal.valueOf(5));
        when(analytics.getTopProducts(from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                5, EngagementMetric.SCORE)).thenReturn(List.of(engagement));

        var result = tools.getTopViewedProducts(from, to, null, null);

        assertEquals(List.of(engagement), result.data());
        verify(analytics).getTopProducts(from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                5, EngagementMetric.SCORE);
    }

    @Test
    void productEngagementReturnsOneExactProductAggregate() {
        ProductEngagement engagement = new ProductEngagement(
                12L, "Quạt", null, 20, 4, 2, 10, 1, BigDecimal.valueOf(5));
        when(analytics.getProductEngagement("Quạt", from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(engagement));

        var result = tools.getProductEngagement("Quạt", from, to);

        assertEquals(engagement, result.data());
    }

    @Test
    void engagementFunnelUsesAnalyticsService() {
        TrackingFunnel funnel = new TrackingFunnel(20, 4, 2);
        when(analytics.getFunnel(from.atStartOfDay(), to.plusDays(1).atStartOfDay())).thenReturn(funnel);

        var result = tools.getEngagementFunnel(from, to);

        assertEquals(funnel, result.data());
    }

    @Test
    void analyticsToolsRejectMissingDatesBeforeCallingService() {
        var topProducts = tools.getTopViewedProducts(null, to, 5, EngagementMetric.VIEWS);
        var funnel = tools.getEngagementFunnel(from, null);

        assertEquals(List.of(), topProducts.data());
        assertNull(funnel.data());
        verifyNoInteractions(analytics);
    }
}
