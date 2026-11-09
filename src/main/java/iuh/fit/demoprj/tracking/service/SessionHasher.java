package iuh.fit.demoprj.tracking.service;

import iuh.fit.demoprj.tracking.config.TrackingProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SessionHasher {
    private static final Logger logger = LoggerFactory.getLogger(SessionHasher.class);

    private final String salt;

    public SessionHasher(TrackingProperties properties) {
        this.salt = properties.getSessionHashSalt();
        if (properties.isEnabled() && (salt == null || salt.isBlank())) {
            logger.warn("Product tracking is enabled but TRACKING_SALT is missing; tracking events will be dropped.");
        }
    }

    public String hash(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be blank.");
        }
        if (!isConfigured()) {
            throw new IllegalStateException("TRACKING_SALT must be configured before hashing session IDs.");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((sessionId + salt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public boolean isConfigured() {
        return salt != null && !salt.isBlank();
    }
}
