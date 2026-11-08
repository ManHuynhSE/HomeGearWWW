package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iuh.fit.demoprj.ai.index.PolicyDocumentLoader;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

class PolicyDocumentLoaderTest {

    @Test
    void loadsPolicyChunksWithRuntimeConfigurationAndSourceMetadata() {
        var context = new StaticApplicationContext();
        var loader = new PolicyDocumentLoader(
                context,
                BigDecimal.valueOf(500_000), BigDecimal.valueOf(30_000),
                true, false, false, false,
                "0900000000", "help@example.com");

        var documents = loader.load();

        assertFalse(documents.isEmpty());
        assertTrue(documents.stream().allMatch(document ->
                "policy".equals(document.getMetadata().get("type"))));
        assertTrue(documents.stream().anyMatch(document ->
                "shipping.md".equals(document.getMetadata().get("source"))
                        && document.getText().contains("500,000 ₫")
                        && document.getText().contains("30,000 ₫")));
        assertTrue(documents.stream().anyMatch(document ->
                "payments.md".equals(document.getMetadata().get("source"))
                        && document.getText().contains("đang được hỗ trợ")));
        assertTrue(documents.stream().anyMatch(document ->
                "returns.md".equals(document.getMetadata().get("source"))
                        && document.getText().contains("0900000000")));
    }
}
