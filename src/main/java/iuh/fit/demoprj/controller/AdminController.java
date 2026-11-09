package iuh.fit.demoprj.controller;

import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.service.OrderService;
import iuh.fit.demoprj.service.AdminService;
import iuh.fit.demoprj.model.entity.CustomerOrder;
import iuh.fit.demoprj.tracking.config.TrackingProperties;
import iuh.fit.demoprj.tracking.model.EngagementMetric;
import iuh.fit.demoprj.tracking.model.ProductEngagement;
import iuh.fit.demoprj.tracking.model.TrackingFunnel;
import iuh.fit.demoprj.tracking.service.AnalyticsService;
import java.math.RoundingMode;
import java.time.Clock;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class AdminController {
    private final CatalogService catalog;
    private final OrderService orders;
    private final iuh.fit.demoprj.repository.OrderItemRepository orderItems;
    private final AdminService admin;
    private final AnalyticsService analytics;
    private final TrackingProperties trackingProperties;
    private final Clock clock;

    public AdminController(CatalogService catalog, OrderService orders,
                           iuh.fit.demoprj.repository.OrderItemRepository orderItems,
                           AdminService admin, AnalyticsService analytics,
                           TrackingProperties trackingProperties, Clock trackingClock) {
        this.catalog = catalog;
        this.orders = orders;
        this.orderItems = orderItems;
        this.admin = admin;
        this.analytics = analytics;
        this.trackingProperties = trackingProperties;
        this.clock = trackingClock;
    }

    @GetMapping("/admin/assistant")
    public String assistant(Model model) {
        model.addAttribute("pageTitle", "Trợ lý AI | HomeGear");
        return "admin-assistant";
    }

    @GetMapping("/admin")
    public String dashboard(@RequestParam(required = false) Long editProduct,
                            @RequestParam(required = false) Long editCategory,
                            @RequestParam(required = false) Long editCustomer,
                            @RequestParam(required = false) String productKeyword,
                            @RequestParam(required = false) String customerKeyword,
                            @RequestParam(required = false) String orderKeyword,
                            @RequestParam(required = false) String orderStatus,
                            @RequestParam(defaultValue = "7") String days,
                            @RequestParam(defaultValue = "SCORE") EngagementMetric metric,
                            Model model) {
        List<CustomerOrder> allOrders = admin.searchOrders(null, null);
        var allProducts = catalog.search(null);
        var allCustomers = admin.findCustomers();
        model.addAttribute("products", catalog.search(productKeyword));
        model.addAttribute("categories", catalog.getCategories());
        model.addAttribute("orders", admin.searchOrders(orderKeyword, orderStatus));
        model.addAttribute("customers", admin.searchCustomers(customerKeyword));
        model.addAttribute("orderStatuses", admin.getOrderStatuses());
        model.addAttribute("revenue", admin.calculateRevenue(allOrders));
        model.addAttribute("productCount", allProducts.size());
        model.addAttribute("customerCount", allCustomers.size());
        model.addAttribute("orderCount", allOrders.size());
        model.addAttribute("lowStockCount", allProducts.stream().filter(product -> product.getStock() <= 5).count());
        model.addAttribute("newOrderCount", allOrders.stream().filter(order -> "NEW".equals(order.getStatus())).count());
        model.addAttribute("shippingOrderCount", allOrders.stream().filter(order -> "SHIPPING".equals(order.getStatus())).count());
        model.addAttribute("completedOrderCount", allOrders.stream().filter(order -> "COMPLETED".equals(order.getStatus())).count());
        model.addAttribute("productKeyword", productKeyword == null ? "" : productKeyword);
        model.addAttribute("customerKeyword", customerKeyword == null ? "" : customerKeyword);
        model.addAttribute("orderKeyword", orderKeyword == null ? "" : orderKeyword);
        model.addAttribute("orderStatus", orderStatus == null ? "" : orderStatus);
        AnalyticsRange analyticsRange = analyticsRange(days);
        List<ProductEngagement> topProducts = analytics.getTopProducts(
                analyticsRange.from(), analyticsRange.to(), 5, metric);
        TrackingFunnel funnel = analytics.getFunnel(analyticsRange.from(), analyticsRange.to());
        model.addAttribute("topEngagements", topProducts);
        model.addAttribute("maxEngagementScore",
                topProducts.stream().mapToLong(ProductEngagement::score).max().orElse(0));
        model.addAttribute("trackingFunnel", funnel);
        model.addAttribute("viewToCartRate", percentage(funnel.addToCart(), funnel.views()));
        model.addAttribute("cartToPurchaseRate", percentage(
                funnel.purchasedQuantity(), funnel.addToCart()));
        model.addAttribute("lowConversionProducts", analytics.getLowConversionProducts(
                analyticsRange.from(), analyticsRange.to(), 10, 5));
        model.addAttribute("selectedDays", analyticsRange.days());
        model.addAttribute("selectedMetric", metric.name());
        model.addAttribute("analyticsPeriodLabel", analyticsRange.label());
        model.addAttribute("clickScoreWeight", trackingProperties.getWeight().getClick());
        model.addAttribute("cartScoreWeight", trackingProperties.getWeight().getAddToCart());
        if (editProduct != null) {
            model.addAttribute("editProduct", catalog.getProduct(editProduct));
            model.addAttribute("productSpecificationsText", catalog.getProductSpecificationsText(editProduct));
        } else {
            model.addAttribute("productSpecificationsText", "");
        }
        if (editCategory != null) model.addAttribute("editCategory", catalog.getCategory(editCategory));
        if (editCustomer != null) model.addAttribute("editCustomer", admin.getCustomer(editCustomer));
        return "admin";
    }

    private String dashboard(Long editProduct, Long editCategory, Long editCustomer,
                             String productKeyword, String customerKeyword, String orderKeyword,
                             String orderStatus, Model model) {
        return dashboard(editProduct, editCategory, editCustomer, productKeyword, customerKeyword,
                orderKeyword, orderStatus, "7", EngagementMetric.SCORE, model);
    }

    private AnalyticsRange analyticsRange(String days) {
        LocalDateTime to = LocalDateTime.now(clock);
        return switch (days == null ? "" : days.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "7" -> new AnalyticsRange(to.minusDays(7), to, "7", "7 ngày gần nhất");
            case "30" -> new AnalyticsRange(to.minusDays(30), to, "30", "30 ngày gần nhất");
            case "all" -> new AnalyticsRange(LocalDateTime.of(1000, 1, 1, 0, 0), to,
                    "all", "Toàn bộ dữ liệu còn lưu");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Khoảng thời gian analytics không hợp lệ.");
        };
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private record AnalyticsRange(LocalDateTime from, LocalDateTime to, String days, String label) {
    }

    @PostMapping("/admin/categories/save")
    public String saveCategory(@RequestParam(required = false) Long id,
                               @RequestParam String name, Model model) {
        try {
            catalog.saveCategory(id, name);
            return "redirect:/admin?success=category";
        } catch (IllegalArgumentException ex) {
            return dashboard(null, id, null, null, null, null, null, model.addAttribute("error", ex.getMessage()));
        }
    }

    @PostMapping("/admin/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, Model model) {
        try {
            catalog.deleteCategory(id);
            return "redirect:/admin?success=category-deleted";
        } catch (IllegalArgumentException ex) {
            return dashboard(null, null, null, null, null, null, null, model.addAttribute("error", ex.getMessage()));
        }
    }

    @PostMapping("/admin/products/save")
    public String saveProduct(@RequestParam(required = false) Long id,
                              @RequestParam String name,
                              @RequestParam BigDecimal price,
                              @RequestParam(required = false) BigDecimal originalPrice,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime promotionStartsAt,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime promotionEndsAt,
                              @RequestParam int stock,
                              @RequestParam(defaultValue = "") String description,
                              @RequestParam(defaultValue = "") String imageUrl,
                              @RequestParam(defaultValue = "") String imageAttribution,
                              @RequestParam(defaultValue = "") String imageSourceUrl,
                              @RequestParam(defaultValue = "") String specificationText,
                              @RequestParam Long categoryId,
                              @RequestParam(required = false) Boolean active, Model model) {
        try {
            boolean isActive = active != null ? active : id == null || catalog.getProduct(id).isActive();
            catalog.saveProduct(id, name, price, stock, originalPrice,
                    promotionStartsAt, promotionEndsAt, description, imageUrl,
                    imageAttribution, imageSourceUrl, specificationText, categoryId, isActive);
            return "redirect:/admin?success=product";
        } catch (IllegalArgumentException ex) {
            String page = dashboard(id, null, null, null, null, null, null,
                    model.addAttribute("error", ex.getMessage()));
            model.addAttribute("productSpecificationsText", specificationText);
            return page;
        }
    }

    @PostMapping("/admin/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, Model model) {
        try {
            catalog.deleteProduct(id, orderItems);
            return "redirect:/admin?success=product-deleted";
        } catch (IllegalArgumentException ex) {
            return dashboard(null, null, null, null, null, null, null, model.addAttribute("error", ex.getMessage()));
        }
    }

    @PostMapping("/admin/customers/save")
    public String saveCustomer(@RequestParam Long id, @RequestParam String fullName,
                               @RequestParam String email,
                               @RequestParam(defaultValue = "") String password, Model model) {
        try {
            admin.updateCustomer(id, fullName, email, password);
            return "redirect:/admin?success=customer";
        } catch (IllegalArgumentException ex) {
            return dashboard(null, null, id, null, null, null, null, model.addAttribute("error", ex.getMessage()));
        }
    }

    @PostMapping("/admin/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id, Model model) {
        try {
            admin.deleteCustomer(id);
            return "redirect:/admin?success=customer-deleted";
        } catch (IllegalArgumentException ex) {
            return dashboard(null, null, null, null, null, null, null, model.addAttribute("error", ex.getMessage()));
        }
    }

    @GetMapping("/admin/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", admin.getOrder(id));
        model.addAttribute("orderStatuses", admin.getOrderStatuses());
        return "admin-order-detail";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id, @RequestParam String status,
                                    Model model) {
        try {
            admin.updateOrderStatus(id, status);
            return "redirect:/admin?success=order";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("order", admin.getOrder(id));
            model.addAttribute("orderStatuses", admin.getOrderStatuses());
            return "admin-order-detail";
        }
    }
}
