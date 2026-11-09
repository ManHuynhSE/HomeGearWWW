package iuh.fit.demoprj;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.controller.HomeController;
import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.repository.OrderItemRepository;
import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.tracking.model.EventSource;
import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.service.TrackingContext;
import iuh.fit.demoprj.tracking.service.TrackingContextFactory;
import iuh.fit.demoprj.tracking.service.TrackingService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

class HomeControllerTrackingTest {
    @Test
    void recordsViewWhenProductDetailRenders() {
        CatalogService catalog = mock(CatalogService.class);
        TrackingService tracking = mock(TrackingService.class);
        TrackingContextFactory contextFactory = mock(TrackingContextFactory.class);
        Product product = new Product("Quạt", BigDecimal.valueOf(100_000), 3,
                "Quạt gia đình", new Category("Điện tử"));
        ReflectionTestUtils.setField(product, "id", 12L);
        when(catalog.getProductDetail(12L)).thenReturn(product);
        when(catalog.getNewArrivals()).thenReturn(List.of());
        when(catalog.getActivePromotions(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(catalog.getRelatedProducts(product)).thenReturn(List.of());
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.addHeader("User-Agent", "Mozilla/5.0");
        TrackingContext context = new TrackingContext(null, "a".repeat(64), false, false);
        when(contextFactory.create(null, session.getId(), "Mozilla/5.0")).thenReturn(context);
        HomeController controller = new HomeController(catalog, mock(OrderItemRepository.class),
                tracking, contextFactory);

        controller.legacyDetail(12L, new ExtendedModelMap(), request, null);

        verify(tracking).record(12L, EventType.PRODUCT_VIEW, EventSource.DETAIL, context);
    }
}
