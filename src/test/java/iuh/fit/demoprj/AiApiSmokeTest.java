package iuh.fit.demoprj;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.jayway.jsonpath.JsonPath;
import iuh.fit.demoprj.ai.index.VectorIndexService;
import iuh.fit.demoprj.service.CatalogService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfSystemProperty(named = "ai.smoke", matches = "true")
class AiApiSmokeTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CatalogService catalog;
    @Autowired
    private VectorIndexService vectorIndex;

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
    void storeChatUsesLiveCatalogToolForPriceAndStock() throws Exception {
        var product = catalog.searchProductsForAssistant("Nồi chiên không dầu 5L", null, null, null)
                .stream().findFirst().orElseThrow();

        String response = mockMvc.perform(post("/api/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Hãy dùng công cụ tra cứu để tìm chính xác sản phẩm Nồi chiên không dầu 5L. Cho tôi biết giá bán hiện tại và số lượng tồn kho."}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String reply = JsonPath.read(response, "$.reply");
        String replyDigits = reply.replaceAll("\\D", "");
        String currentPrice = product.getPrice().stripTrailingZeros().toPlainString().replaceAll("\\D", "");

        assertTrue(reply.contains(product.getName()), "The answer should identify the product returned by the catalog.");
        assertTrue(replyDigits.contains(currentPrice), "The answer should include the current database price.");
        assertTrue(reply.matches("(?s).*?(?<!\\d)" + product.getStock() + "(?!\\d).*"),
                "The answer should include the current database stock.");
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
    void adminAssistantUsesLiveStockTool() throws Exception {
        var product = catalog.findLowStockProducts(100).stream().findFirst().orElseThrow();

        String response = mockMvc.perform(post("/api/admin/assistant")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Dùng công cụ getLowStockProducts với ngưỡng 100 để kiểm tra dữ liệu hiện tại. Hãy nêu tên và số lượng tồn kho của ít nhất một sản phẩm."}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String reply = JsonPath.read(response, "$.reply");

        assertTrue(reply.contains(product.getName()), "The answer should include a product returned by the stock tool.");
        assertTrue(reply.matches("(?s).*?(?<!\\d)" + product.getStock() + "(?!\\d).*"),
                "The answer should include the current database stock.");
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

    @Test
    void storeChatUsesSemanticProductSearchAndRetrievedPolicy() throws Exception {
        vectorIndex.reindexAll();
        var product = catalog.searchProductsForAssistant("Nồi chiên không dầu 5L", null, null, null)
                .stream().findFirst().orElseThrow();

        String productResponse = mockMvc.perform(post("/api/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Tôi muốn nấu món giòn nhưng hạn chế dầu mỡ. Hãy dùng searchProductsSemantic để tìm sản phẩm phù hợp và cho biết giá cùng tồn kho."}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String productReply = JsonPath.read(productResponse, "$.reply");
        String currentPrice = product.getPrice().stripTrailingZeros().toPlainString().replaceAll("\\D", "");
        assertTrue(productReply.contains(product.getName()));
        assertTrue(productReply.replaceAll("\\D", "").contains(currentPrice));
        assertTrue(productReply.matches("(?s).*?(?<!\\d)" + product.getStock() + "(?!\\d).*"));

        String policyResponse = mockMvc.perform(post("/api/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"HomeGear đổi trả trong bao lâu? Hãy trả lời theo chính sách cửa hàng, không suy đoán."}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String policyReply = JsonPath.read(policyResponse, "$.reply");
        policyReply = policyReply.toLowerCase();
        assertTrue(policyReply.contains("chưa"));
        assertTrue(policyReply.contains("cấu hình") || policyReply.contains("xác nhận"));
    }
}
