package iuh.fit.demoprj.ai.web;

import iuh.fit.demoprj.ai.service.AiChatService;
import iuh.fit.demoprj.ai.web.dto.ChatRequest;
import iuh.fit.demoprj.ai.web.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final AiChatService aiChatService;

    public ChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(aiChatService.answerStoreQuestion(request.message()));
    }
}
