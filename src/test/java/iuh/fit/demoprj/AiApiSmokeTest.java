package iuh.fit.demoprj;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfSystemProperty(named = "ai.smoke", matches = "true")
class AiApiSmokeTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void storeChatEndpointReturnsModelReply() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Chào bạn, HomeGear bán những thiết bị gia dụng nào?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", not(blankOrNullString())));
    }

    @Test
    void adminAssistantIsNotAvailableToCustomers() throws Exception {
        mockMvc.perform(post("/api/admin/assistant")
                        .with(csrf())
                        .with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Tóm tắt tình hình cửa hàng\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseTheReadOnlyAssistantEndpoint() throws Exception {
        mockMvc.perform(post("/api/admin/assistant")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Tôi cần cung cấp thông tin gì để xem thống kê doanh thu?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", not(blankOrNullString())));
    }

    @Test
    void storeChatPolitelyRedirectsOutOfScopeQuestions() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Giải giúp tôi bài toán 12 cộng 8 bằng bao nhiêu?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value(org.hamcrest.Matchers.containsStringIgnoringCase("HomeGear")));
    }
}
