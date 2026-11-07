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
@RequestMapping("/api/admin/assistant")
public class AdminAssistantController {
    private final AiChatService aiChatService;

    public AdminAssistantController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping
    public ChatResponse assist(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(aiChatService.answerAdminQuestion(request.message()));
    }
}
