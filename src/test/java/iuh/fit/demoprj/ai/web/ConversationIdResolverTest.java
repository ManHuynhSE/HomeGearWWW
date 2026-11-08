package iuh.fit.demoprj.ai.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpSession;

class ConversationIdResolverTest {
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final ConversationIdResolver resolver = new ConversationIdResolver(accounts);

    @Test
    void guestConversationIdsAreScopedToSession() {
        assertEquals("store:guest:session-1",
                resolver.resolveStore(null, new MockHttpSession(null, "session-1")));
        assertNotEquals(
                resolver.resolveStore(null, new MockHttpSession(null, "session-1")),
                resolver.resolveStore(null, new MockHttpSession(null, "session-2")));
    }

    @Test
    void customerAndAdminUseSeparateConversationNamespaces() {
        UserAccount customer = account(18L, "customer@example.com", Role.CUSTOMER);
        UserAccount admin = account(18L, "admin@example.com", Role.ADMIN);
        when(accounts.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(accounts.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        assertEquals("store:customer:18", resolver.resolveStore(authentication("customer@example.com", "CUSTOMER"), null));
        assertEquals("assistant:admin:18", resolver.resolveAdmin(authentication("admin@example.com", "ADMIN")));
    }

    @Test
    void adminConversationRejectsAnAccountWithTheWrongRole() {
        when(accounts.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(Optional.of(account(18L, "customer@example.com", Role.CUSTOMER)));

        assertThrows(AccessDeniedException.class,
                () -> resolver.resolveAdmin(authentication("customer@example.com", "ADMIN")));
    }

    private static UserAccount account(long id, String email, Role role) {
        UserAccount account = new UserAccount("Test", email, "password", role);
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private static UsernamePasswordAuthenticationToken authentication(String email, String role) {
        return new UsernamePasswordAuthenticationToken(
                email, "password", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
