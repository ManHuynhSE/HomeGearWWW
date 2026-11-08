package iuh.fit.demoprj.ai.web;

import iuh.fit.demoprj.ai.service.AiChatService;
import iuh.fit.demoprj.ai.security.ChatRateLimiter;
import iuh.fit.demoprj.ai.web.dto.ChatRequest;
import iuh.fit.demoprj.ai.web.dto.ChatResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/assistant")
public class AdminAssistantController {
    private final AiChatService aiChatService;
    private final ConversationIdResolver conversationIds;
    private final ChatRateLimiter rateLimiter;

    public AdminAssistantController(AiChatService aiChatService, ConversationIdResolver conversationIds,
                                    ChatRateLimiter rateLimiter) {
        this.aiChatService = aiChatService;
        this.conversationIds = conversationIds;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ChatResponse assist(@Valid @RequestBody ChatRequest request, Authentication authentication,
                               HttpServletRequest servletRequest) {
        rateLimiter.check(authentication, servletRequest.getRemoteAddr());
        String conversationId = conversationIds.resolveAdmin(authentication);
        return new ChatResponse(aiChatService.answerAdminQuestion(request.message(), conversationId));
    }

    @PostMapping("/conversation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void newConversation(Authentication authentication) {
        aiChatService.clearConversation(conversationIds.resolveAdmin(authentication));
    }
}
