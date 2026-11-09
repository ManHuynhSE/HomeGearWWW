package iuh.fit.demoprj.tracking.service;

import iuh.fit.demoprj.tracking.config.TrackingProperties;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class BotDetector {
    private final TrackingProperties properties;

    public BotDetector(TrackingProperties properties) {
        this.properties = properties;
    }

    public boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return true;
        }
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        return properties.getBotKeywords().stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }
}
