package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.CustomerOrder;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.CustomerOrderRepository;
import iuh.fit.demoprj.repository.ProductRepository;
import iuh.fit.demoprj.repository.UserAccountRepository;
import iuh.fit.demoprj.service.OrderService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrderServiceTest {

    @Test
    void repeatedCheckoutTokenReturnsExistingOrderWithoutChargingStockTwice() {
        ProductRepository products = mock(ProductRepository.class);
        CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        OrderService service = new OrderService(products, orders, accounts);
        UserAccount customer = new UserAccount("Khách hàng", "customer@example.com", "password", Role.CUSTOMER);
        ReflectionTestUtils.setField(customer, "id", 7L);
        Product product = new Product("Quạt", BigDecimal.valueOf(200_000), 5,
                "Quạt gia đình", new Category("Điện tử"));
        ReflectionTestUtils.setField(product, "id", 11L);
        CustomerOrder existingOrder = new CustomerOrder(customer, BigDecimal.valueOf(230_000));

        when(accounts.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(products.findForCheckoutById(11L)).thenReturn(Optional.of(product));
        when(orders.findByCheckoutToken("checkout-token")).thenReturn(Optional.empty());
        when(orders.findForCheckoutTokenByCheckoutToken("checkout-token"))
                .thenReturn(Optional.empty(), Optional.of(existingOrder));
        when(orders.save(any(CustomerOrder.class))).thenReturn(existingOrder);

        CustomerOrder created = service.checkout("customer@example.com", Map.of(11L, 1),
                "Nguyễn An", "0912345678", "1 Đường A, TP HCM",
                BigDecimal.valueOf(500_000), BigDecimal.valueOf(30_000), "checkout-token");
        CustomerOrder repeated = service.checkout("customer@example.com", Map.of(11L, 1),
                "Nguyễn An", "0912345678", "1 Đường A, TP HCM",
                BigDecimal.valueOf(500_000), BigDecimal.valueOf(30_000), "checkout-token");

        assertSame(existingOrder, created);
        assertSame(created, repeated);
        assertEquals(4, product.getStock());
        verify(orders, times(1)).save(any(CustomerOrder.class));
    }
}
