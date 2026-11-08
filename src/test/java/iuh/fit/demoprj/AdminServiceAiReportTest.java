package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.CustomerOrder;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.CustomerOrderRepository;
import iuh.fit.demoprj.repository.OrderItemRepository;
import iuh.fit.demoprj.repository.UserAccountRepository;
import iuh.fit.demoprj.service.AdminService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AdminServiceAiReportTest {
    private UserAccountRepository accounts;
    private CustomerOrderRepository orders;
    private OrderItemRepository orderItems;
    private AdminService admin;

    @BeforeEach
    void setUp() {
        accounts = mock(UserAccountRepository.class);
        orders = mock(CustomerOrderRepository.class);
        orderItems = mock(OrderItemRepository.class);
        admin = new AdminService(accounts, orders, orderItems, mock(PasswordEncoder.class));
    }

    @Test
    void revenueCountsCompletedOrdersWithinInclusiveDateRangeOnly() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 7);
        when(orders.sumCompletedRevenueBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                .thenReturn(BigDecimal.valueOf(120_000));

        BigDecimal revenue = admin.calculateRevenue(from, to);

        assertEquals(BigDecimal.valueOf(120_000), revenue);
    }

    @Test
    void orderSummaryReturnsCountsAndAggregatesWithoutCustomerDetails() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 7);
        var completed = mock(CustomerOrderRepository.OrderStatusAggregate.class);
        when(completed.getStatus()).thenReturn("COMPLETED");
        when(completed.getOrderCount()).thenReturn(1L);
        when(completed.getTotal()).thenReturn(BigDecimal.valueOf(120_000));
        var cancelled = mock(CustomerOrderRepository.OrderStatusAggregate.class);
        when(cancelled.getStatus()).thenReturn("CANCELLED");
        when(cancelled.getOrderCount()).thenReturn(1L);
        when(cancelled.getTotal()).thenReturn(BigDecimal.valueOf(450_000));
        when(orders.summarizeOrdersBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(completed, cancelled));

        var summary = admin.getOrdersSummary(from, to);

        assertEquals(2, summary.totalOrders());
        assertEquals(1L, summary.ordersByStatus().get("COMPLETED"));
        assertEquals(BigDecimal.valueOf(120_000), summary.nonCancelledOrderValue());
        assertEquals(BigDecimal.valueOf(120_000), summary.completedRevenue());
    }

    @Test
    void topSellingProductsUsesBoundedQueryAndInclusiveDates() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 7);
        Product product = new Product("Quạt", BigDecimal.valueOf(200_000), 5,
                "Quạt", new Category("Điện tử"));
        OrderItemRepository.ProductSalesProjection projection =
                mock(OrderItemRepository.ProductSalesProjection.class);
        when(projection.getProduct()).thenReturn(product);
        when(projection.getUnitsSold()).thenReturn(6L);
        when(orderItems.findTopSellingProducts(from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                PageRequest.of(0, 3))).thenReturn(List.of(projection));

        var results = admin.getTopSellingProducts(from, to, 3);

        assertEquals(6, results.get(0).unitsSold());
        assertEquals(product, results.get(0).product());
        verify(orderItems).findTopSellingProducts(from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(), PageRequest.of(0, 3));
    }

    @Test
    void invalidDateRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                admin.getOrdersSummary(LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 7)));
    }

}
