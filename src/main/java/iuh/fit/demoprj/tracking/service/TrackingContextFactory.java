package iuh.fit.demoprj.tracking.service;

import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.repository.UserAccountRepository;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class TrackingContextFactory {
    private static final Logger logger = LoggerFactory.getLogger(TrackingContextFactory.class);

    private final SessionHasher sessionHasher;
    private final BotDetector botDetector;
    private final UserAccountRepository accounts;

    public TrackingContextFactory(SessionHasher sessionHasher, BotDetector botDetector,
                                  UserAccountRepository accounts) {
        this.sessionHasher = sessionHasher;
        this.botDetector = botDetector;
        this.accounts = accounts;
    }

    public TrackingContext create(Authentication authentication, String sessionId, String userAgent) {
        boolean admin = hasRole(authentication, Role.ADMIN);
        boolean authenticatedCustomer = hasRole(authentication, Role.CUSTOMER);
        Long userId = null;
        if (authenticatedCustomer) {
            Optional<UserAccount> account;
            try {
                account = accounts.findByEmailIgnoreCase(authentication.getName());
            } catch (RuntimeException exception) {
                logger.warn("Skipping tracking because the authenticated customer could not be resolved.", exception);
                return new TrackingContext(null, null, botDetector.isBot(userAgent), admin);
            }
            if (account.isPresent() && account.get().getRole() == Role.ADMIN) {
                admin = true;
            } else if (account.isPresent() && account.get().getRole() == Role.CUSTOMER) {
                userId = account.get().getId();
            }
        }

        String sessionHash = sessionHasher.isConfigured()
                && (!authenticatedCustomer || userId != null)
                ? sessionHasher.hash(sessionId)
                : null;
        return new TrackingContext(userId, sessionHash, botDetector.isBot(userAgent), admin);
    }

    private static boolean hasRole(Authentication authentication, Role role) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null && !authentication.getName().isBlank()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_" + role.name()));
    }
}
