package iuh.fit.demoprj.tracking.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.demoprj.tracking.config.TrackingProperties;
import iuh.fit.demoprj.tracking.model.EventSource;
import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.service.TrackingContext;
import iuh.fit.demoprj.tracking.service.TrackingContextFactory;
import iuh.fit.demoprj.tracking.service.TrackingService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

class TrackingControllerTest {
    private final TrackingProperties properties = new TrackingProperties();
    private final TrackingContextFactory contextFactory = mock(TrackingContextFactory.class);
    private final TrackingRateLimiter rateLimiter = mock(TrackingRateLimiter.class);
    private final TrackingService tracking = mock(TrackingService.class);
    private final TrackingContext context = new TrackingContext(null, "a".repeat(64), false, false);
    private TrackingController controller;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        controller = new TrackingController(properties, contextFactory, rateLimiter, tracking);
        session = new MockHttpSession();
        when(contextFactory.create(null, session.getId(), "Mozilla/5.0")).thenReturn(context);
        when(rateLimiter.tryAcquire(context.sessionHash())).thenReturn(true);
    }

    @Test
    void recordsOnlyProductClicksWithValidProductAndSource() {
        MockHttpServletRequest request = request("""
                {"event":"PRODUCT_CLICK","productId":12,"source":"LISTING"}
                """);

        controller.track(request, session, null);

        verify(tracking).record(12L, EventType.PRODUCT_CLICK, EventSource.LISTING, context);
    }

    @Test
    void ignoresClientAttemptsToSubmitServerEventsAndInvalidProducts() {
        controller.track(request("""
                {"event":"ADD_TO_CART","productId":12,"source":"LISTING"}
                """), session, null);
        controller.track(request("""
                {"event":"PRODUCT_CLICK","productId":0,"source":"LISTING"}
                """), session, null);
        controller.track(request("""
                {"event":"PRODUCT_CLICK","productId":12,"source":"UNKNOWN"}
                """), session, null);
        controller.track(request("""
                {"event":"PRODUCT_CLICK","productId":12}
                """), session, null);

        verify(tracking, never()).record(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresOversizedMalformedAndRateLimitedRequests() {
        controller.track(request("{".repeat(513)), session, null);
        controller.track(request("{invalid"), session, null);
        when(rateLimiter.tryAcquire(context.sessionHash())).thenReturn(false);
        controller.track(request("""
                {"event":"PRODUCT_CLICK","productId":12,"source":"LISTING"}
                """), session, null);

        verify(tracking, never()).record(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest request(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json");
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.setContent(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return request;
    }
}
