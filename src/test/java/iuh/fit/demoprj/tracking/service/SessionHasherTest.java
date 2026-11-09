package iuh.fit.demoprj.tracking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import iuh.fit.demoprj.tracking.config.TrackingProperties;
import org.junit.jupiter.api.Test;

class SessionHasherTest {
    @Test
    void hashesSessionIdAsStableSha256HexUsingConfiguredSalt() {
        TrackingProperties firstProperties = properties("first-secret");
        TrackingProperties sameProperties = properties("first-secret");
        TrackingProperties otherProperties = properties("other-secret");

        String hash = new SessionHasher(firstProperties).hash("session-123");

        assertEquals(64, hash.length());
        assertEquals(hash, new SessionHasher(sameProperties).hash("session-123"));
        assertNotEquals(hash, new SessionHasher(otherProperties).hash("session-123"));
        assertNotEquals(hash, new SessionHasher(firstProperties).hash("session-456"));
    }

    @Test
    void doesNotHashWithoutSaltOrSessionId() {
        assertThrows(IllegalStateException.class, () -> new SessionHasher(properties(" ")).hash("session-123"));
        assertThrows(IllegalArgumentException.class, () -> new SessionHasher(properties("secret")).hash(" "));
    }

    private static TrackingProperties properties(String salt) {
        TrackingProperties properties = new TrackingProperties();
        properties.setSessionHashSalt(salt);
        return properties;
    }
}
