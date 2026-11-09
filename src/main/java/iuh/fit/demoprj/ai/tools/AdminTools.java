package iuh.fit.demoprj.ai.tools;

import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.service.AdminService;
import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.tracking.model.EngagementMetric;
import iuh.fit.demoprj.tracking.model.ProductEngagement;
import iuh.fit.demoprj.tracking.model.TrackingFunnel;
import iuh.fit.demoprj.tracking.service.AnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminTools {
    private final CatalogService catalog;
    private final AdminService admin;
    private final AnalyticsService analytics;
    private final AiToolInvocationLogger invocationLogger;

    public AdminTools(CatalogService catalog, AdminService admin, AnalyticsService analytics,
                      AiToolInvocationLogger invocationLogger) {
        this.catalog = catalog;
        this.admin = admin;
        this.analytics = analytics;
        this.invocationLogger = invocationLogger;
    }

    @Tool(name = "getLowStockProducts", description = """
            Liệt kê tối đa 20 sản phẩm có tồn kho không vượt ngưỡng đã cho.
            Dùng ngưỡng mặc định 5 nếu quản trị viên không nêu con số.
            """)
    public StoreResult<List<ProductStock>> getLowStockProducts(
            @ToolParam(description = "Ngưỡng tồn kho từ 0 đến 100", required = false) Integer threshold) {
        invocationLogger.logInvocation("getLowStockProducts");
        int stockThreshold = threshold == null ? 5 : threshold;
        if (stockThreshold < 0 || stockThreshold > 100) {
            return new StoreResult<>("Ngưỡng tồn kho phải từ 0 đến 100.", List.of());
        }
        List<ProductStock> products = catalog.findLowStockProducts(stockThreshold).stream()
                .map(AdminTools::toStock)
                .toList();
        return new StoreResult<>(products.isEmpty() ? "Không có sản phẩm dưới ngưỡng tồn kho."
                : "Sản phẩm tồn kho thấp.", products);
    }

    @Tool(name = "getStockByProduct", description = "Tra cứu số lượng tồn kho hiện tại theo tên sản phẩm.")
    public StoreResult<List<ProductStock>> getStockByProduct(
            @ToolParam(description = "Tên hoặc từ khóa sản phẩm") String productName) {
        invocationLogger.logInvocation("getStockByProduct");
        if (productName == null || productName.isBlank()) {
            return new StoreResult<>("Vui lòng cung cấp tên sản phẩm.", List.of());
        }
        List<ProductStock> products = catalog.searchProductsForAssistant(productName, null, null, null).stream()
                .map(AdminTools::toStock)
                .toList();
        return new StoreResult<>(products.isEmpty() ? "Không tìm thấy sản phẩm." : "Tồn kho sản phẩm.", products);
    }

    @Tool(name = "countProductsByCategory", description = "Đếm số sản phẩm trong từng danh mục.")
    public StoreResult<List<CatalogService.CategoryProductCount>> countProductsByCategory() {
        invocationLogger.logInvocation("countProductsByCategory");
        return new StoreResult<>("Số sản phẩm theo danh mục.", catalog.countProductsByCategory());
    }

    @Tool(name = "getRevenue", description = """
            Tính doanh thu từ các đơn đã hoàn tất trong khoảng ngày, tính cả hai ngày đầu/cuối.
            Ngày phải theo ISO yyyy-MM-dd; nếu thiếu mốc ngày, hãy hỏi quản trị viên trước khi gọi.
            """)
    public StoreResult<Revenue> getRevenue(
            @ToolParam(description = "Ngày bắt đầu, định dạng ISO yyyy-MM-dd", required = false) LocalDate fromDate,
            @ToolParam(description = "Ngày kết thúc, định dạng ISO yyyy-MM-dd", required = false) LocalDate toDate) {
        invocationLogger.logInvocation("getRevenue");
        if (!validDateRange(fromDate, toDate)) {
            return new StoreResult<>("Vui lòng cung cấp khoảng ngày ISO hợp lệ, ngày kết thúc không trước ngày bắt đầu.", null);
        }
        return new StoreResult<>("Doanh thu đơn hoàn tất trong khoảng ngày.",
                new Revenue(fromDate, toDate, admin.calculateRevenue(fromDate, toDate)));
    }

    @Tool(name = "getTopSellingProducts", description = """
            Xem tối đa 10 sản phẩm bán chạy nhất theo số lượng trong khoảng ngày.
            Ngày phải theo ISO yyyy-MM-dd; nếu thiếu mốc ngày, hãy hỏi quản trị viên trước khi gọi.
            Đơn đã hủy không được tính.
            """)
    public StoreResult<List<TopProduct>> getTopSellingProducts(
            @ToolParam(description = "Ngày bắt đầu, định dạng ISO yyyy-MM-dd", required = false) LocalDate fromDate,
            @ToolParam(description = "Ngày kết thúc, định dạng ISO yyyy-MM-dd", required = false) LocalDate toDate,
            @ToolParam(description = "Số lượng kết quả từ 1 đến 10, mặc định 5", required = false) Integer limit) {
        invocationLogger.logInvocation("getTopSellingProducts");
        int resultLimit = limit == null ? 5 : limit;
        if (!validDateRange(fromDate, toDate)) {
            return new StoreResult<>("Vui lòng cung cấp khoảng ngày ISO hợp lệ, ngày kết thúc không trước ngày bắt đầu.", List.of());
        }
        if (resultLimit < 1 || resultLimit > 10) {
            return new StoreResult<>("Số lượng kết quả phải từ 1 đến 10.", List.of());
        }
        List<TopProduct> products = admin.getTopSellingProducts(fromDate, toDate, resultLimit).stream()
                .map(sale -> new TopProduct(toStock(sale.product()), sale.unitsSold()))
                .toList();
        return new StoreResult<>(products.isEmpty() ? "Không có sản phẩm bán trong khoảng ngày."
                : "Sản phẩm bán chạy trong khoảng ngày.", products);
    }

    @Tool(name = "getOrdersSummary", description = """
            Tổng hợp số đơn theo trạng thái, giá trị đơn không bị hủy và doanh thu đơn hoàn tất trong khoảng ngày.
            Ngày phải theo ISO yyyy-MM-dd; nếu thiếu mốc ngày, hãy hỏi quản trị viên trước khi gọi.
            """)
    public StoreResult<AdminService.OrdersSummary> getOrdersSummary(
            @ToolParam(description = "Ngày bắt đầu, định dạng ISO yyyy-MM-dd", required = false) LocalDate fromDate,
            @ToolParam(description = "Ngày kết thúc, định dạng ISO yyyy-MM-dd", required = false) LocalDate toDate) {
        invocationLogger.logInvocation("getOrdersSummary");
        if (!validDateRange(fromDate, toDate)) {
            return new StoreResult<>("Vui lòng cung cấp khoảng ngày ISO hợp lệ, ngày kết thúc không trước ngày bắt đầu.", null);
        }
        return new StoreResult<>("Tổng hợp đơn hàng trong khoảng ngày.", admin.getOrdersSummary(fromDate, toDate));
    }

    @Tool(name = "getTopViewedProducts", description = """
            Xem tối đa 10 sản phẩm được quan tâm trong khoảng ngày, xếp theo SCORE, CLICKS,
            ADD_TO_CART hoặc VIEWS. Mặc định limit=5, metric=SCORE.
            Ngày dùng định dạng ISO yyyy-MM-dd; nếu thiếu mốc ngày, hãy hỏi quản trị viên trước khi gọi.
            Điểm mặc định = click × 1 + thêm giỏ × 3; dùng đúng điểm trả về nếu trọng số đã được cấu hình khác.
            """)
    public StoreResult<List<ProductEngagement>> getTopViewedProducts(
            @ToolParam(description = "Ngày bắt đầu, định dạng ISO yyyy-MM-dd", required = false) LocalDate fromDate,
            @ToolParam(description = "Ngày kết thúc, định dạng ISO yyyy-MM-dd", required = false) LocalDate toDate,
            @ToolParam(description = "Số lượng kết quả từ 1 đến 10, mặc định 5", required = false) Integer limit,
            @ToolParam(description = "SCORE, CLICKS, ADD_TO_CART hoặc VIEWS; mặc định SCORE", required = false)
            EngagementMetric metric) {
        invocationLogger.logInvocation("getTopViewedProducts");
        int resultLimit = limit == null ? 5 : limit;
        AnalyticsRange range = analyticsRange(fromDate, toDate);
        if (range == null) {
            return new StoreResult<>(invalidAnalyticsRangeMessage(), List.of());
        }
        if (resultLimit < 1 || resultLimit > 10) {
            return new StoreResult<>("Số lượng kết quả phải từ 1 đến 10.", List.of());
        }

        List<ProductEngagement> result = analytics.getTopProducts(
                range.from(), range.to(), resultLimit, metric == null ? EngagementMetric.SCORE : metric);
        return new StoreResult<>(result.isEmpty() ? "Không có dữ liệu tương tác trong khoảng ngày."
                : "Sản phẩm được quan tâm trong khoảng ngày.", result);
    }

    @Tool(name = "getProductEngagement", description = """
            Tra cứu một sản phẩm khớp tên và các chỉ số xem, click, thêm giỏ, điểm,
            số lượng mua, tỷ lệ chuyển đổi trong khoảng ngày.
            Ngày dùng định dạng ISO yyyy-MM-dd; nếu thiếu mốc ngày, hãy hỏi quản trị viên trước khi gọi.
            """)
    public StoreResult<ProductEngagement> getProductEngagement(
            @ToolParam(description = "Tên hoặc một phần tên sản phẩm") String productName,
            @ToolParam(description = "Ngày bắt đầu, định dạng ISO yyyy-MM-dd", required = false) LocalDate fromDate,
            @ToolParam(description = "Ngày kết thúc, định dạng ISO yyyy-MM-dd", required = false) LocalDate toDate) {
        invocationLogger.logInvocation("getProductEngagement");
        AnalyticsRange range = analyticsRange(fromDate, toDate);
        if (range == null) {
            return new StoreResult<>(invalidAnalyticsRangeMessage(), null);
        }
        if (productName == null || productName.isBlank()) {
            return new StoreResult<>("Vui lòng cung cấp tên sản phẩm.", null);
        }

        List<ProductEngagement> matches = analytics.getProductEngagement(
                productName, range.from(), range.to());
        ProductEngagement exactMatch = matches.stream()
                .filter(item -> item.productName().equalsIgnoreCase(productName.trim()))
                .findFirst()
                .orElse(null);
        if (exactMatch != null) {
            return new StoreResult<>("Chỉ số tương tác sản phẩm trong khoảng ngày.", exactMatch);
        }
        if (matches.size() == 1) {
            return new StoreResult<>("Chỉ số tương tác sản phẩm trong khoảng ngày.", matches.get(0));
        }
        return new StoreResult<>(matches.isEmpty()
                ? "Không tìm thấy sản phẩm khớp tên."
                : "Có nhiều sản phẩm khớp tên; vui lòng cung cấp tên đầy đủ hơn.", null);
    }

    @Tool(name = "getEngagementFunnel", description = """
            Tổng hợp lượt xem sản phẩm, số lần thêm giỏ thành công và số lượng đã mua
            trong khoảng ngày. Đơn đã hủy không được tính vào số lượng mua.
            Ngày dùng định dạng ISO yyyy-MM-dd; nếu thiếu mốc ngày, hãy hỏi quản trị viên trước khi gọi.
            """)
    public StoreResult<TrackingFunnel> getEngagementFunnel(
            @ToolParam(description = "Ngày bắt đầu, định dạng ISO yyyy-MM-dd", required = false) LocalDate fromDate,
            @ToolParam(description = "Ngày kết thúc, định dạng ISO yyyy-MM-dd", required = false) LocalDate toDate) {
        invocationLogger.logInvocation("getEngagementFunnel");
        AnalyticsRange range = analyticsRange(fromDate, toDate);
        if (range == null) {
            return new StoreResult<>(invalidAnalyticsRangeMessage(), null);
        }
        return new StoreResult<>("Phễu tương tác trong khoảng ngày.",
                analytics.getFunnel(range.from(), range.to()));
    }

    private AnalyticsRange analyticsRange(LocalDate fromDate, LocalDate toDate) {
        if (!validDateRange(fromDate, toDate)) {
            return null;
        }
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.plusDays(1).atStartOfDay();
        return new AnalyticsRange(from, to);
    }

    private String invalidAnalyticsRangeMessage() {
        return "Vui lòng cung cấp khoảng ngày ISO hợp lệ, ngày kết thúc không trước ngày bắt đầu.";
    }

    private boolean validDateRange(LocalDate fromDate, LocalDate toDate) {
        return fromDate != null && toDate != null && !toDate.isBefore(fromDate)
                && !toDate.equals(LocalDate.MAX);
    }

    private static ProductStock toStock(Product product) {
        return new ProductStock(product.getId(), product.getName(), product.getStock(),
                product.getCategory().getName());
    }

    public record StoreResult<T>(String message, T data) {
    }

    public record ProductStock(Long id, String name, int stock, String category) {
    }

    public record TopProduct(ProductStock product, long unitsSold) {
    }

    public record Revenue(LocalDate fromDate, LocalDate toDate, BigDecimal completedRevenue) {
    }

    private record AnalyticsRange(LocalDateTime from, LocalDateTime to) {
    }
}
