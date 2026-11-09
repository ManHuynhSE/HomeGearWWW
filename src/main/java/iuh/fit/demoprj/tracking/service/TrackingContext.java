package iuh.fit.demoprj.tracking.service;

public record TrackingContext(Long userId, String sessionHash, boolean isBot, boolean isAdmin) {
}
