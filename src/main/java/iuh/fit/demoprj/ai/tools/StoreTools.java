package iuh.fit.demoprj.ai.tools;

import iuh.fit.demoprj.model.entity.CustomerOrder;
import iuh.fit.demoprj.model.entity.OrderItem;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.model.entity.ProductSpecification;
import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class StoreTools {
    private static final int MAX_ORDER_ITEMS = 10;

    private final CatalogService catalog;
    private final OrderService orders;
    private final AiToolInvocationLogger invocationLogger;

    public StoreTools(CatalogService catalog, OrderService orders, AiToolInvocationLogger invocationLogger) {
        this.catalog = catalog;
        this.orders = orders;
        this.invocationLogger = invocationLogger;
    }

    @Tool(name = "searchProducts", description = """
            Tìm tối đa 10 sản phẩm HomeGear theo từ khóa, tên danh mục và khoảng giá.
            Dùng để tra cứu tên, giá hiện tại, tồn kho và danh mục; bỏ qua các điều kiện không biết.
            """)
    public ToolResult<List<ProductSummary>> searchProducts(
            @ToolParam(description = "Từ khóa tên hoặc mô tả sản phẩm", required = false) String keyword,
            @ToolParam(description = "Tên danh mục chính xác nếu khách nêu", required = false) String categoryName,
            @ToolParam(description = "Giá tối thiểu, đơn vị VND", required = false) BigDecimal minPrice,
            @ToolParam(description = "Giá tối đa, đơn vị VND", required = false) BigDecimal maxPrice) {
        invocationLogger.logInvocation("searchProducts");
        List<ProductSummary> found = catalog.searchProductsForAssistant(keyword, categoryName, minPrice, maxPrice)
                .stream().map(StoreTools::toSummary).toList();
        return new ToolResult<>(found.isEmpty() ? "Không tìm thấy sản phẩm phù hợp." : "Sản phẩm khớp tiêu chí.", found);
    }

    @Tool(name = "getProductDetail", description = """
            Lấy thông tin thật của một sản phẩm theo ID, gồm mô tả, giá, tồn kho và thông số kỹ thuật.
            Chỉ gọi khi đã có ID sản phẩm từ kết quả tra cứu trước đó hoặc do khách cung cấp.
            """)
    public ToolResult<ProductDetail> getProductDetail(
            @ToolParam(description = "ID sản phẩm cần xem") Long productId) {
        invocationLogger.logInvocation("getProductDetail");
        if (productId == null || productId <= 0) {
            return new ToolResult<>("ID sản phẩm không hợp lệ.", null);
        }
        try {
            return new ToolResult<>("Thông tin sản phẩm.", toDetail(catalog.getProductDetail(productId)));
        } catch (IllegalArgumentException exception) {
            return new ToolResult<>("Không tìm thấy sản phẩm.", null);
        }
    }

    @Tool(name = "compareProducts", description = """
            So sánh 2 hoặc 3 sản phẩm theo ID đã biết, gồm giá, tồn kho, danh mục và thông số.
            Không tự đoán ID; hãy tìm sản phẩm trước nếu chưa có ID.
            """)
    public ToolResult<List<ProductDetail>> compareProducts(
            @ToolParam(description = "Danh sách gồm 2 hoặc 3 ID sản phẩm") List<Long> productIds) {
        invocationLogger.logInvocation("compareProducts");
        if (productIds == null || productIds.size() < 2 || productIds.size() > 3
                || productIds.stream().anyMatch(id -> id == null || id <= 0)
                || productIds.stream().distinct().count() != productIds.size()) {
            return new ToolResult<>("Cần chọn 2 hoặc 3 ID sản phẩm hợp lệ và không trùng nhau.", List.of());
        }
        try {
            List<ProductDetail> comparison = productIds.stream()
                    .map(catalog::getProductDetail)
                    .map(StoreTools::toDetail)
                    .toList();
            return new ToolResult<>("Thông tin so sánh sản phẩm.", comparison);
        } catch (IllegalArgumentException exception) {
            return new ToolResult<>("Không tìm thấy một hoặc nhiều sản phẩm cần so sánh.", List.of());
        }
    }

    @Tool(name = "getMyOrders", description = """
            Lấy tối đa 10 đơn hàng gần nhất của chính khách đang đăng nhập.
            Không nhận tên, email hoặc ID người dùng; nếu khách là Guest, trả hướng dẫn đăng nhập.
            """)
    public ToolResult<List<OrderSummary>> getMyOrders() {
        invocationLogger.logInvocation("getMyOrders");
        String customerEmail = currentCustomerEmail();
        if (customerEmail == null) {
            return new ToolResult<>("Vui lòng đăng nhập bằng tài khoản khách hàng để xem đơn hàng.", List.of());
        }
        List<OrderSummary> customerOrders = orders.findRecentOrdersForCustomer(customerEmail)
                .stream().map(StoreTools::toOrderSummary).toList();
        return new ToolResult<>(customerOrders.isEmpty() ? "Tài khoản chưa có đơn hàng." : "Đơn hàng của bạn.",
                customerOrders);
    }

    @Tool(name = "getMyOrderStatus", description = """
            Kiểm tra trạng thái một đơn hàng của chính khách đang đăng nhập.
            Không nhận userId/email; đơn hàng được kiểm tra thuộc tài khoản hiện tại trước khi trả kết quả.
            """)
    public ToolResult<OrderSummary> getMyOrderStatus(
            @ToolParam(description = "Mã đơn hàng cần kiểm tra") Long orderId) {
        invocationLogger.logInvocation("getMyOrderStatus");
        String customerEmail = currentCustomerEmail();
        if (customerEmail == null) {
            return new ToolResult<>("Vui lòng đăng nhập bằng tài khoản khách hàng để kiểm tra đơn hàng.", null);
        }
        if (orderId == null || orderId <= 0) {
            return new ToolResult<>("Mã đơn hàng không hợp lệ.", null);
        }
        try {
            CustomerOrder order = orders.findOrderForCustomer(orderId, customerEmail);
            return new ToolResult<>("Thông tin đơn hàng của bạn.", toOrderSummary(order));
        } catch (IllegalArgumentException exception) {
            return new ToolResult<>("Không tìm thấy đơn hàng thuộc tài khoản của bạn.", null);
        }
    }

    private String currentCustomerEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch("ROLE_CUSTOMER"::equals)) {
            return null;
        }
        return authentication.getName();
    }

    private static ProductSummary toSummary(Product product) {
        return new ProductSummary(product.getId(), product.getName(), product.getPrice(),
                product.getStock(), product.getCategory().getName(), product.getCanonicalPath());
    }

    private static ProductDetail toDetail(Product product) {
        String description = product.getDescription() == null ? "" : product.getDescription();
        if (description.length() > 700) description = description.substring(0, 700);
        List<Specification> specifications = product.getSpecifications().stream()
                .limit(20)
                .map(specification -> new Specification(
                        specification.getLabel(), specification.getValue(), specification.getUnit()))
                .toList();
        return new ProductDetail(product.getId(), product.getName(), description, product.getPrice(),
                product.getOriginalPrice(), product.getStock(), product.getCategory().getName(),
                product.getCanonicalPath(), specifications);
    }

    private static OrderSummary toOrderSummary(CustomerOrder order) {
        List<OrderLine> items = order.getItems().stream()
                .limit(MAX_ORDER_ITEMS)
                .map(StoreTools::toOrderLine)
                .toList();
        return new OrderSummary(order.getId(), order.getCreatedAt(), order.getStatus(), order.getTotal(), items);
    }

    private static OrderLine toOrderLine(OrderItem item) {
        return new OrderLine(item.getProduct().getName(), item.getQuantity(), item.getUnitPrice());
    }

    public record ToolResult<T>(String message, T data) {
    }

    public record ProductSummary(Long id, String name, BigDecimal price, int stock,
                                 String category, String productPath) {
    }

    public record ProductDetail(Long id, String name, String description, BigDecimal price,
                                BigDecimal originalPrice, int stock, String category,
                                String productPath, List<Specification> specifications) {
    }

    public record Specification(String name, String value, String unit) {
    }

    public record OrderSummary(Long orderId, LocalDateTime createdAt, String status,
                               BigDecimal total, List<OrderLine> items) {
    }

    public record OrderLine(String productName, int quantity, BigDecimal unitPrice) {
    }
}
