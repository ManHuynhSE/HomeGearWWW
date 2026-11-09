package iuh.fit.demoprj.tracking.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.tracking")
public class TrackingProperties {
    private boolean enabled = true;

    @Min(1)
    private int dedupeWindowMinutes = 30;

    @Valid
    private final Weight weight = new Weight();

    @Min(1)
    private int retentionDays = 90;

    @Min(1)
    private int rateLimitPerMinute = 60;

    private List<String> botKeywords = new ArrayList<>(
            List.of("bot", "crawler", "spider", "slurp", "headless"));

    private String sessionHashSalt = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDedupeWindowMinutes() {
        return dedupeWindowMinutes;
    }

    public void setDedupeWindowMinutes(int dedupeWindowMinutes) {
        this.dedupeWindowMinutes = dedupeWindowMinutes;
    }

    public Weight getWeight() {
        return weight;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public List<String> getBotKeywords() {
        return botKeywords;
    }

    public void setBotKeywords(List<String> botKeywords) {
        this.botKeywords = botKeywords;
    }

    public String getSessionHashSalt() {
        return sessionHashSalt;
    }

    public void setSessionHashSalt(String sessionHashSalt) {
        this.sessionHashSalt = sessionHashSalt;
    }

    public static class Weight {
        @Min(0)
        private int click = 1;

        @Min(0)
        private int addToCart = 3;

        public int getClick() {
            return click;
        }

        public void setClick(int click) {
            this.click = click;
        }

        public int getAddToCart() {
            return addToCart;
        }

        public void setAddToCart(int addToCart) {
            this.addToCart = addToCart;
        }
    }
}
