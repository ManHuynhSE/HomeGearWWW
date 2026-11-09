package iuh.fit.demoprj.tracking.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iuh.fit.demoprj.tracking.config.TrackingProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class BotDetectorTest {
    @Test
    void detectsConfiguredBotKeywordsWithoutCaseSensitivity() {
        TrackingProperties properties = new TrackingProperties();
        properties.setBotKeywords(List.of("crawler", "headless"));
        BotDetector detector = new BotDetector(properties);

        assertTrue(detector.isBot("ExampleCrawler/1.0"));
        assertTrue(detector.isBot("Mozilla Headless Browser"));
        assertFalse(detector.isBot("Mozilla/5.0 (Windows NT 10.0)"));
    }

    @Test
    void treatsMissingUserAgentAsSuspicious() {
        BotDetector detector = new BotDetector(new TrackingProperties());

        assertTrue(detector.isBot(null));
        assertTrue(detector.isBot("  "));
    }
}
