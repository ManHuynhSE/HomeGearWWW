package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.repository.CategoryRepository;
import iuh.fit.demoprj.repository.ProductRepository;
import iuh.fit.demoprj.tracking.model.EventSource;
import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.model.ProductEvent;
import iuh.fit.demoprj.tracking.repository.ProductEventRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import iuh.fit.demoprj.ai.tools.AdminTools;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAssistantPageTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminTools adminTools;
    @Autowired
    private ProductRepository products;
    @Autowired
    private CategoryRepository categories;
    @Autowired
    private ProductEventRepository productEvents;

    @Test
    void assistantPageIsAvailableToAdmins() throws Exception {
        mockMvc.perform(get("/admin/assistant").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trợ lý AI")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/admin/assistant")));
    }

    @Test
    void adminDashboardRendersAnalyticsAndServerSideFilters() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN"))
                        .param("days", "30")
                        .param("metric", "CLICKS"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Top 5 sản phẩm được quan tâm")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Phễu mua sắm")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"days\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"metric\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"30\" selected=\"selected\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"CLICKS\" selected=\"selected\"")));
    }

    @Test
    @Transactional
    void adminDashboardRendersTrackedProductsAndTheirMetrics() throws Exception {
        String unique = UUID.randomUUID().toString();
        Category category = categories.saveAndFlush(new Category("Dashboard-" + unique));
        Product product = products.saveAndFlush(new Product(
                "Tracked " + unique, BigDecimal.TEN, 5, "Dashboard test product", category));
        LocalDateTime viewedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(5);
        productEvents.saveAndFlush(new ProductEvent(product.getId(), EventType.PRODUCT_VIEW, null,
                "a".repeat(64), EventSource.DETAIL, viewedAt));
        productEvents.saveAndFlush(new ProductEvent(product.getId(), EventType.PRODUCT_CLICK, null,
                "b".repeat(64), EventSource.HOME, viewedAt.plusMinutes(1)));
        productEvents.saveAndFlush(new ProductEvent(product.getId(), EventType.ADD_TO_CART, null,
                "c".repeat(64), EventSource.LISTING, viewedAt.plusMinutes(2)));

        MvcResult result = mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();
        String productLink = "/admin?editProduct=" + product.getId()
                + "&amp;days=7&amp;metric=SCORE#products";
        int linkIndex = html.indexOf(productLink);
        assertTrue(linkIndex >= 0);
        assertTrue(html.contains("Tracked " + unique));
        String productRow = html.substring(html.lastIndexOf("<tr", linkIndex), html.indexOf("</tr>", linkIndex));
        assertTrue(productRow.contains("<td>1</td>"));
        assertTrue(productRow.contains("admin-engagement-score"));
        assertTrue(productRow.contains("admin-score-track"));
    }

    @Test
    void dashboardAnalyticsIsAdminOnly() throws Exception {
        mockMvc.perform(get("/admin").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void assistantPageIsNotAvailableToCustomers() throws Exception {
        mockMvc.perform(get("/admin/assistant").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotCallTheAdminAssistantApi() throws Exception {
        mockMvc.perform(post("/api/admin/assistant").with(user("customer").roles("CUSTOMER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"xem báo cáo\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAssistantApiRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/admin/assistant").with(user("admin").roles("ADMIN"))
                        .contentType("application/json")
                        .content("{\"message\":\"xem báo cáo\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotTriggerAdminReindex() throws Exception {
        mockMvc.perform(post("/admin/ai/reindex").with(user("customer").roles("CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void guestChatRequiresCsrfAndIsOtherwisePublic() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"message\":\"tìm nồi chiên\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/chat").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"tìm nồi chiên\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void trackingEndpointRequiresCsrfButIgnoresUntrustedEventTypes() throws Exception {
        mockMvc.perform(post("/api/track")
                        .contentType("application/json")
                        .content("{\"event\":\"PRODUCT_CLICK\",\"productId\":12,\"source\":\"LISTING\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/track").with(csrf())
                        .contentType("application/json")
                        .content("{\"event\":\"ADD_TO_CART\",\"productId\":12,\"source\":\"LISTING\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void storePagesRenderTheChatWidgetAndCsrfToken() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"hg-store-chat-panel\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"csrf-token\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"csrf-header-name\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/chat.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/tracker.js")));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotInvokeAdminToolDirectly() {
        assertThrows(AccessDeniedException.class, adminTools::countProductsByCategory);
    }
}
