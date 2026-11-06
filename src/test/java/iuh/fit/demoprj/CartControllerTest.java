package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.controller.CartController;
import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.service.OrderService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class CartControllerTest {

    @Test
    void addUsesRequestedQuantityAndReturnsToProductWhenAsked() {
        CatalogService catalog = mock(CatalogService.class);
        Product product = productWithId(25L, 5);
        when(catalog.getProduct(25L)).thenReturn(product);
        CartController controller = new CartController(catalog, mock(OrderService.class));
        MockHttpSession session = new MockHttpSession();

        String redirect = controller.add(25L, 3, "detail", session, new RedirectAttributesModelMap());

        assertEquals("redirect:/san-pham/bo-am-nuoc-25", redirect);
        Object cart = session.getAttribute("cart");
        assertTrue(cart instanceof Map<?, ?>);
        assertEquals(3, ((Map<?, ?>) cart).get(25L));
    }

    @Test
    void addRejectsQuantityAboveAvailableStockWithoutChangingCart() {
        CatalogService catalog = mock(CatalogService.class);
        Product product = productWithId(25L, 3);
        when(catalog.getProduct(25L)).thenReturn(product);
        CartController controller = new CartController(catalog, mock(OrderService.class));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", Map.of(25L, 2));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String redirect = controller.add(25L, 2, "cart", session, redirectAttributes);

        assertEquals("redirect:/cart", redirect);
        assertEquals(Map.of(25L, 2), session.getAttribute("cart"));
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("cartError"));
    }

    private Product productWithId(Long id, int stock) {
        Product product = new Product("Bộ ấm nước", BigDecimal.valueOf(100_000), stock,
                "Ấm nước", new Category("Nhà bếp"));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
