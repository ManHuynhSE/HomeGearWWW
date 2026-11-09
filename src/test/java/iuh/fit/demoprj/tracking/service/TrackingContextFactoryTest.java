package iuh.fit.demoprj.tracking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.UserAccountRepository;
import iuh.fit.demoprj.tracking.config.TrackingProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

class TrackingContextFactoryTest {
    @Test
    void resolvesCustomerIdAndNeverTreatsAnUnresolvedCustomerAsGuest() {
        TrackingProperties properties = new TrackingProperties();
        properties.setSessionHashSalt("tracking-secret");
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccount account = new UserAccount("Customer", "customer@example.com", "password", Role.CUSTOMER);
        ReflectionTestUtils.setField(account, "id", 42L);
        when(accounts.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(account));
        TrackingContextFactory factory = new TrackingContextFactory(
                new SessionHasher(properties), new BotDetector(properties), accounts);
        var authentication = new UsernamePasswordAuthenticationToken(
                "customer@example.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        TrackingContext context = factory.create(authentication, "session-1", "Mozilla/5.0");
        assertEquals(42L, context.userId());
        assertEquals(64, context.sessionHash().length());
        assertFalse(context.isBot());
        assertFalse(context.isAdmin());

        when(accounts.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        var unresolved = new UsernamePasswordAuthenticationToken(
                "missing@example.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        assertNull(factory.create(unresolved, "session-1", "Mozilla/5.0").sessionHash());
    }

    @Test
    void marksAdminsAndMissingUserAgentsAsExcluded() {
        TrackingProperties properties = new TrackingProperties();
        properties.setSessionHashSalt("tracking-secret");
        TrackingContextFactory factory = new TrackingContextFactory(
                new SessionHasher(properties), new BotDetector(properties), mock(UserAccountRepository.class));
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        TrackingContext context = factory.create(authentication, "session-1", null);

        assertTrue(context.isAdmin());
        assertTrue(context.isBot());
        assertNull(context.userId());
    }
}
