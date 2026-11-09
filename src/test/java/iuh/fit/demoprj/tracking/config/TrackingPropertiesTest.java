package iuh.fit.demoprj.tracking.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TrackingPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TrackingConfiguration.class);

    @Test
    void bindsTrackingDefaultsAndEnvironmentSalt() {
        contextRunner.withPropertyValues("app.tracking.session-hash-salt=local-test-salt")
                .run(context -> {
                    assertTrue(context.isRunning());
                    TrackingProperties properties = context.getBean(TrackingProperties.class);
                    assertTrue(properties.isEnabled());
                    assertEquals(30, properties.getDedupeWindowMinutes());
                    assertEquals(1, properties.getWeight().getClick());
                    assertEquals(3, properties.getWeight().getAddToCart());
                    assertEquals(90, properties.getRetentionDays());
                    assertEquals(60, properties.getRateLimitPerMinute());
                    assertEquals(List.of("bot", "crawler", "spider", "slurp", "headless"),
                            properties.getBotKeywords());
                    assertEquals("local-test-salt", properties.getSessionHashSalt());
                });
    }

    @Test
    void bindsOverridesAndRejectsInvalidLimits() {
        contextRunner.withPropertyValues(
                        "app.tracking.enabled=false",
                        "app.tracking.dedupe-window-minutes=15",
                        "app.tracking.weight.click=2",
                        "app.tracking.weight.add-to-cart=4",
                        "app.tracking.retention-days=30",
                        "app.tracking.rate-limit-per-minute=25",
                        "app.tracking.bot-keywords=bot,scanner")
                .run(context -> {
                    assertTrue(context.isRunning());
                    TrackingProperties properties = context.getBean(TrackingProperties.class);
                    assertFalse(properties.isEnabled());
                    assertEquals(15, properties.getDedupeWindowMinutes());
                    assertEquals(2, properties.getWeight().getClick());
                    assertEquals(4, properties.getWeight().getAddToCart());
                    assertEquals(30, properties.getRetentionDays());
                    assertEquals(25, properties.getRateLimitPerMinute());
                    assertEquals(List.of("bot", "scanner"), properties.getBotKeywords());
                });

        contextRunner.withPropertyValues("app.tracking.retention-days=0")
                .run(context -> assertThat(context).hasFailed());
    }
}
