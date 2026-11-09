package iuh.fit.demoprj.tracking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.CustomerOrder;
import iuh.fit.demoprj.model.entity.OrderItem;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.CategoryRepository;
import iuh.fit.demoprj.repository.CustomerOrderRepository;
import iuh.fit.demoprj.repository.OrderItemRepository;
import iuh.fit.demoprj.repository.ProductRepository;
import iuh.fit.demoprj.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AnalyticsRepositoryTest {
    @Autowired
    private OrderItemRepository orderItems;
    @Autowired
    private ProductRepository products;
    @Autowired
    private CategoryRepository categories;
    @Autowired
    private UserAccountRepository accounts;
    @Autowired
    private CustomerOrderRepository orders;

    @Test
    void sumsPurchasesInHalfOpenRangeAndExcludesCancelledOrders() {
        String unique = UUID.randomUUID().toString();
        UserAccount customer = accounts.saveAndFlush(new UserAccount(
                "Analytics Test", unique + "@example.test", "password", Role.CUSTOMER));
        Category category = categories.saveAndFlush(new Category("Analytics-" + unique));
        Product product = products.saveAndFlush(new Product(
                "Analytics product", BigDecimal.TEN, 10, "", category));
        LocalDateTime from = LocalDateTime.of(2099, 2, 1, 0, 0);
        LocalDateTime to = from.plusDays(1);

        saveOrder(customer, product, from, "COMPLETED", 3);
        saveOrder(customer, product, from.plusMinutes(1), "CANCELLED", 5);
        saveOrder(customer, product, to, "COMPLETED", 7);

        var purchased = orderItems.sumPurchasedQuantityBetween(from, to);

        assertEquals(1, purchased.size());
        assertEquals(product.getId(), purchased.get(0).getProductId());
        assertEquals(3L, purchased.get(0).getPurchasedQuantity());
    }

    private void saveOrder(UserAccount customer, Product product, LocalDateTime createdAt,
                           String status, int quantity) {
        CustomerOrder order = new CustomerOrder(customer, BigDecimal.TEN);
        ReflectionTestUtils.setField(order, "createdAt", createdAt);
        order.setStatus(status);
        order.addItem(new OrderItem(product, quantity, BigDecimal.TEN));
        orders.saveAndFlush(order);
    }
}
