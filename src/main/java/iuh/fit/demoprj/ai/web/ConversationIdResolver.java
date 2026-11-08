package iuh.fit.demoprj.ai.web;

import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.UserAccountRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class ConversationIdResolver {
    private static final String STORE_CUSTOMER_PREFIX = "store:customer:";
    private static final String STORE_GUEST_PREFIX = "store:guest:";
    private static final String ADMIN_PREFIX = "assistant:admin:";

    private final UserAccountRepository accounts;

    public ConversationIdResolver(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    public String resolveStore(Authentication authentication, HttpSession session) {
        if (hasRole(authentication, Role.CUSTOMER)) {
            return STORE_CUSTOMER_PREFIX + findAccount(authentication, Role.CUSTOMER).getId();
        }
        return guestStoreConversationId(Objects.requireNonNull(session, "session").getId());
    }

    public String resolveAdmin(Authentication authentication) {
        if (!hasRole(authentication, Role.ADMIN)) {
            throw new AccessDeniedException("Admin conversation requires an administrator account.");
        }
        return ADMIN_PREFIX + findAccount(authentication, Role.ADMIN).getId();
    }

    public static String guestStoreConversationId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Guest session ID cannot be blank.");
        }
        return STORE_GUEST_PREFIX + sessionId;
    }

    private UserAccount findAccount(Authentication authentication, Role role) {
        UserAccount account = accounts.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated account could not be resolved."));
        if (account.getRole() != role) {
            throw new AccessDeniedException("Authenticated account role does not match the chat endpoint.");
        }
        return account;
    }

    private boolean hasRole(Authentication authentication, Role role) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role.name()).equals(authority.getAuthority()));
    }
}
