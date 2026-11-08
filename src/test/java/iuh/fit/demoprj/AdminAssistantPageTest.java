package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import iuh.fit.demoprj.ai.tools.AdminTools;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAssistantPageTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminTools adminTools;

    @Test
    void assistantPageIsAvailableToAdmins() throws Exception {
        mockMvc.perform(get("/admin/assistant").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trợ lý AI")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/admin/assistant")));
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
    void storePagesRenderTheChatWidgetAndCsrfToken() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"hg-store-chat-panel\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"csrf-token\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/chat.js")));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotInvokeAdminToolDirectly() {
        assertThrows(AccessDeniedException.class, adminTools::countProductsByCategory);
    }
}
