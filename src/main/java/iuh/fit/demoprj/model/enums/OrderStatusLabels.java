package iuh.fit.demoprj.model.enums;

import java.util.Map;

public final class OrderStatusLabels {
    private static final Map<String, String> LABELS = Map.of(
            "NEW", "Mới",
            "CONFIRMED", "Đã xác nhận",
            "SHIPPING", "Đang giao",
            "COMPLETED", "Hoàn tất",
            "CANCELLED", "Đã hủy"
    );

    private OrderStatusLabels() {
    }

    public static Map<String, String> all() {
        return LABELS;
    }

    public static String label(String status) {
        return LABELS.getOrDefault(status, status);
    }
}
