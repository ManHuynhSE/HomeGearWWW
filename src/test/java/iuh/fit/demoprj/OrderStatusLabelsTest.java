package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;

import iuh.fit.demoprj.model.enums.OrderStatusLabels;
import org.junit.jupiter.api.Test;

class OrderStatusLabelsTest {
    @Test
    void translatesSupportedStatusesAndPreservesUnknownValues() {
        assertEquals("Mới", OrderStatusLabels.label("NEW"));
        assertEquals("Đã xác nhận", OrderStatusLabels.label("CONFIRMED"));
        assertEquals("Đang giao", OrderStatusLabels.label("SHIPPING"));
        assertEquals("Hoàn tất", OrderStatusLabels.label("COMPLETED"));
        assertEquals("Đã hủy", OrderStatusLabels.label("CANCELLED"));
        assertEquals("UNKNOWN", OrderStatusLabels.label("UNKNOWN"));
    }
}
