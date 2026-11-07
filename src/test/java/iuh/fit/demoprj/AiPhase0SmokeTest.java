package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@EnabledIfSystemProperty(named = "ai.smoke", matches = "true")
class AiPhase0SmokeTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void geminiChatClientReturnsAResponse() {
        String response = chatClientBuilder.build()
                .prompt()
                .user("Reply with the single word OK.")
                .call()
                .content();

        assertNotNull(response);
        assertFalse(response.isBlank());
    }
}
