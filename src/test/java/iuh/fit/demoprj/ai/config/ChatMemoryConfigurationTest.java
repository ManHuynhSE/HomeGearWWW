package iuh.fit.demoprj.ai.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

class ChatMemoryConfigurationTest {
    @Test
    void memoryKeepsTheMostRecentTwentyMessagesAndCanBeCleared() {
        ChatMemory memory = new ChatMemoryConfiguration().chatMemory();
        for (int turn = 0; turn < 12; turn++) {
            memory.add("conversation", List.of(
                    new UserMessage("user-" + turn),
                    new AssistantMessage("assistant-" + turn)));
        }

        List<String> contents = memory.get("conversation").stream()
                .map(message -> message.getText())
                .toList();
        assertEquals(20, contents.size());
        assertFalse(contents.contains("user-0"));
        assertFalse(contents.contains("assistant-0"));

        memory.clear("conversation");
        assertEquals(List.of(), memory.get("conversation"));
    }
}
