package iuh.fit.demoprj.ai.web;

import iuh.fit.demoprj.ai.service.AiChatService;
import iuh.fit.demoprj.ai.security.ChatRateLimiter;
import iuh.fit.demoprj.ai.web.dto.ChatRequest;
import iuh.fit.demoprj.ai.web.dto.ChatResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final AiChatService aiChatService;
    private final ConversationIdResolver conversationIds;
    private final ChatRateLimiter rateLimiter;

    public ChatController(AiChatService aiChatService, ConversationIdResolver conversationIds,
                          ChatRateLimiter rateLimiter) {
        this.aiChatService = aiChatService;
        this.conversationIds = conversationIds;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, Authentication authentication,
                             HttpSession session, HttpServletRequest servletRequest) {
        rateLimiter.check(authentication, servletRequest.getRemoteAddr());
        String conversationId = conversationIds.resolveStore(authentication, session);
        return new ChatResponse(aiChatService.answerStoreQuestion(request.message(), conversationId));
    }

    @PostMapping("/conversation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void newConversation(Authentication authentication, HttpSession session) {
        aiChatService.clearConversation(conversationIds.resolveStore(authentication, session));
    }
}
