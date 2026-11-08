package iuh.fit.demoprj.ai.tools;

import iuh.fit.demoprj.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AiToolInvocationLogger {
    private static final Logger logger = LoggerFactory.getLogger(AiToolInvocationLogger.class);

    private final UserAccountRepository accounts;

    public AiToolInvocationLogger(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    public void logInvocation(String toolName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = "guest";
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            String username = authentication.getName();
            userId = username == null || username.isBlank()
                    ? "unresolved"
                    : accounts.findByEmailIgnoreCase(username)
                            .map(account -> String.valueOf(account.getId()))
                            .orElse("unresolved");
        }
        logger.info("AI tool invocation: name={} userId={}", toolName, userId);
    }
}
