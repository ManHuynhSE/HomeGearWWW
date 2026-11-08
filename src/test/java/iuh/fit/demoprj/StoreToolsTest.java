package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.ai.tools.StoreTools;
import iuh.fit.demoprj.ai.tools.AiToolInvocationLogger;
import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.CustomerOrder;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreToolsTest {
    @Mock
    private CatalogService catalog;
    @Mock
    private OrderService orders;
    @Mock
    private AiToolInvocationLogger invocationLogger;

    private StoreTools tools;
    private Product product;

    @BeforeEach
    void setUp() {
        tools = new StoreTools(catalog, orders, invocationLogger);
        product = new Product("Nồi chiên", BigDecimal.valueOf(1_500_000), 4,
                "Nồi chiên gia đình", new Category("Nhà bếp"));
        ReflectionTestUtils.setField(product, "id", 12L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchProductsReturnsOnlyThePublicProductSummary() {
        when(catalog.searchProductsForAssistant("nồi", null, null, null)).thenReturn(List.of(product));

        var result = tools.searchProducts("nồi", null, null, null);

        assertEquals(12L, result.data().get(0).id());
        assertEquals("Nhà bếp", result.data().get(0).category());
        assertEquals("/san-pham/noi-chien-12", result.data().get(0).productPath());
    }

    @Test
    void productDetailIncludesSpecificationsReturnedByCatalogService() {
        when(catalog.getProductDetail(12L)).thenReturn(product);

        var result = tools.getProductDetail(12L);

        assertEquals(12L, result.data().id());
        assertEquals(product.getPrice(), result.data().price());
        assertEquals("/san-pham/noi-chien-12", result.data().productPath());
    }

    @Test
    void compareRequiresTwoOrThreeDistinctProductIds() {
        var result = tools.compareProducts(List.of(12L));

        assertEquals(List.of(), result.data());
        verifyNoInteractions(catalog, orders);
    }

    @Test
    void compareMapsDetailsForEachRequestedProduct() {
        Product secondProduct = new Product("Nồi cơm", BigDecimal.valueOf(900_000), 7,
                "Nồi cơm gia đình", new Category("Nhà bếp"));
        ReflectionTestUtils.setField(secondProduct, "id", 13L);
        when(catalog.getProductDetail(12L)).thenReturn(product);
        when(catalog.getProductDetail(13L)).thenReturn(secondProduct);

        var result = tools.compareProducts(List.of(12L, 13L));

        assertEquals(2, result.data().size());
        assertEquals("Nồi cơm", result.data().get(1).name());
    }

    @Test
    void guestCannotReadOrdersAndReceivesLoginGuidance() {
        var result = tools.getMyOrders();

        assertEquals("Vui lòng đăng nhập bằng tài khoản khách hàng để xem đơn hàng.", result.message());
        assertEquals(List.of(), result.data());
        verifyNoInteractions(orders);
    }

    @Test
    void customerOrderLookupUsesAuthenticatedEmailAndRequestedOrderId() {
        authenticateCustomer();
        CustomerOrder order = new CustomerOrder(
                new UserAccount("Khách", "customer@example.com", "password", Role.CUSTOMER),
                BigDecimal.valueOf(500_000));
        ReflectionTestUtils.setField(order, "id", 44L);
        when(orders.findOrderForCustomer(44L, "customer@example.com")).thenReturn(order);

        var result = tools.getMyOrderStatus(44L);

        assertEquals(44L, result.data().orderId());
        verify(orders).findOrderForCustomer(44L, "customer@example.com");
    }

    @Test
    void customerOrderListUsesTheAuthenticatedAccountOnly() {
        authenticateCustomer();
        CustomerOrder order = new CustomerOrder(
                new UserAccount("Khách", "customer@example.com", "password", Role.CUSTOMER),
                BigDecimal.valueOf(500_000));
        ReflectionTestUtils.setField(order, "id", 44L);
        when(orders.findRecentOrdersForCustomer("customer@example.com")).thenReturn(List.of(order));

        var result = tools.getMyOrders();

        assertEquals(44L, result.data().get(0).orderId());
        verify(orders).findRecentOrdersForCustomer("customer@example.com");
    }

    @Test
    void orderFromAnotherCustomerIsReportedAsNotFound() {
        authenticateCustomer();
        when(orders.findOrderForCustomer(44L, "customer@example.com"))
                .thenThrow(new IllegalArgumentException("Không tìm thấy đơn hàng."));

        var result = tools.getMyOrderStatus(44L);

        assertEquals("Không tìm thấy đơn hàng thuộc tài khoản của bạn.", result.message());
        assertNull(result.data());
    }

    private void authenticateCustomer() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.com", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }
}
