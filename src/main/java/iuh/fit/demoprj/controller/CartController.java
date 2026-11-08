package iuh.fit.demoprj.controller;

import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.service.OrderService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {
    private static final String CART = "cart";
    private static final String CHECKOUT_TOKEN = "checkoutToken";
    private final CatalogService catalog;
    private final OrderService orders;

    @Value("${homegear.shipping.free-threshold:500000}")
    private BigDecimal freeShippingThreshold;

    @Value("${homegear.shipping.flat-fee:30000}")
    private BigDecimal shippingFlatFee;

    @Value("${homegear.store.payment.cod-enabled:false}")
    private boolean codEnabled;

    public CartController(CatalogService catalog, OrderService orders) {
        this.catalog = catalog;
        this.orders = orders;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> cart(HttpSession session) {
        return (Map<Long, Integer>) session.getAttribute(CART);
    }

    @PostMapping("/cart/add/{id}")
    public String add(@PathVariable Long id, @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(defaultValue = "cart") String intent,
                      HttpSession session, RedirectAttributes redirectAttributes) {
        boolean returnToDetail = "detail".equals(intent);
        if (!returnToDetail && !"cart".equals(intent)) {
            throw new IllegalArgumentException("Thao tác giỏ hàng không hợp lệ.");
        }
        Product product;
        try {
            product = catalog.getActiveProduct(id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("cartError", ex.getMessage());
            return "redirect:/products";
        }
        Map<Long, Integer> cart = cart(session);
        if (cart == null) cart = new LinkedHashMap<>();
        int current = cart.getOrDefault(id, 0);
        if (quantity <= 0) {
            redirectAttributes.addFlashAttribute("cartError", "Số lượng phải lớn hơn 0.");
            return returnToDetail ? "redirect:" + product.getCanonicalPath() : "redirect:/cart";
        }
        long next = (long) current + quantity;
        if (next > product.getStock()) {
            redirectAttributes.addFlashAttribute("cartError",
                    "Chỉ còn " + product.getStock() + " sản phẩm trong kho.");
            return returnToDetail ? "redirect:" + product.getCanonicalPath() : "redirect:/cart";
        }
        cart.put(id, (int) next);
        session.setAttribute(CART, cart);
        redirectAttributes.addFlashAttribute("cartSuccess", "Đã thêm sản phẩm vào giỏ hàng.");
        if (returnToDetail) {
            return "redirect:" + product.getCanonicalPath();
        }
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String view(HttpSession session, Model model) {
        addCartSummary(session, model);
        return "cart";
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam Long productId, @RequestParam int quantity,
                         HttpSession session, RedirectAttributes redirectAttributes) {
        Map<Long, Integer> cart = cart(session);
        try {
            if (cart != null) {
                Product product = catalog.getActiveProduct(productId);
                if (quantity <= 0) cart.remove(productId);
                else if (quantity <= product.getStock()) cart.put(productId, quantity);
                else throw new IllegalArgumentException("Sản phẩm " + product.getName()
                        + " chỉ còn " + product.getStock() + " sản phẩm. "
                        + "Số lượng trong giỏ chưa thay đổi.");
                session.setAttribute(CART, cart);
            }
            redirectAttributes.addFlashAttribute("cartSuccess",
                    quantity <= 0 ? "Đã xóa sản phẩm khỏi giỏ hàng." : "Đã cập nhật giỏ hàng.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("cartError", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove/{id}")
    public String remove(@PathVariable Long id, HttpSession session,
                         RedirectAttributes redirectAttributes) {
        Map<Long, Integer> cart = cart(session);
        if (cart != null && cart.remove(id) != null) {
            session.setAttribute(CART, cart);
            redirectAttributes.addFlashAttribute("cartSuccess", "Đã xóa sản phẩm khỏi giỏ hàng.");
        }
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model) {
        if (cart(session) == null || cart(session).isEmpty()) return "redirect:/cart";
        String checkoutToken = (String) session.getAttribute(CHECKOUT_TOKEN);
        if (checkoutToken == null || checkoutToken.isBlank()) {
            checkoutToken = UUID.randomUUID().toString();
            session.setAttribute(CHECKOUT_TOKEN, checkoutToken);
        }
        model.addAttribute("checkoutToken", checkoutToken);
        model.addAttribute("recipientName", "");
        model.addAttribute("recipientPhone", "");
        model.addAttribute("shippingAddress", "");
        model.addAttribute("codEnabled", codEnabled);
        if (!codEnabled) {
            model.addAttribute("error", "Thanh toán đang tạm ngưng vì cửa hàng chưa bật phương thức khả dụng.");
        }
        addCartSummary(session, model);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String checkoutToken,
                           @RequestParam String recipientName,
                           @RequestParam String recipientPhone,
                           @RequestParam String shippingAddress,
                           HttpSession session, Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";
        if (!checkoutToken.equals(session.getAttribute(CHECKOUT_TOKEN))) {
            model.addAttribute("error", "Phiên thanh toán đã hết hạn. Vui lòng kiểm tra lại giỏ hàng.");
            String activeToken = UUID.randomUUID().toString();
            session.setAttribute(CHECKOUT_TOKEN, activeToken);
            model.addAttribute("checkoutToken", activeToken);
            model.addAttribute("recipientName", "");
            model.addAttribute("recipientPhone", "");
            model.addAttribute("shippingAddress", "");
            addCartSummary(session, model);
            return "checkout";
        }
        if (!codEnabled) {
            model.addAttribute("error", "Thanh toán đang tạm ngưng vì cửa hàng chưa bật phương thức khả dụng.");
            model.addAttribute("recipientName", recipientName);
            model.addAttribute("recipientPhone", recipientPhone);
            model.addAttribute("shippingAddress", shippingAddress);
            model.addAttribute("checkoutToken", checkoutToken);
            model.addAttribute("codEnabled", false);
            addCartSummary(session, model);
            return "checkout";
        }
        Map<Long, Integer> cart = cart(session);
        try {
            var order = orders.checkout(authentication.getName(), cart, recipientName,
                    recipientPhone, shippingAddress, freeShippingThreshold,
                    shippingFlatFee, checkoutToken);
            if (checkoutToken.equals(session.getAttribute(CHECKOUT_TOKEN))) {
                session.removeAttribute(CART);
                session.removeAttribute(CHECKOUT_TOKEN);
            }
            return "redirect:/checkout/success/" + order.getId();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("recipientName", recipientName);
            model.addAttribute("recipientPhone", recipientPhone);
            model.addAttribute("shippingAddress", shippingAddress);
            model.addAttribute("checkoutToken", checkoutToken);
            model.addAttribute("codEnabled", codEnabled);
            addCartSummary(session, model);
            return "checkout";
        }
    }

    @GetMapping("/checkout/success/{id}")
    public String checkoutSuccess(@PathVariable Long id, Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return "redirect:/login";
        try {
            model.addAttribute("order", orders.findOrderForCustomer(id, authentication.getName()));
            return "checkout-success";
        } catch (IllegalArgumentException ex) {
            return "redirect:/orders";
        }
    }

    private void addCartSummary(HttpSession session, Model model) {
        Map<Long, Integer> cart = cart(session);
        List<CartLine> lines = new ArrayList<>();
        if (cart != null) {
            cart.forEach((id, quantity) -> {
                Product product = catalog.getProduct(id);
                lines.add(new CartLine(product, quantity,
                        product.getPrice().multiply(BigDecimal.valueOf(quantity))));
            });
        }
        BigDecimal subtotal = lines.stream().map(CartLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = calculateShippingFee(subtotal);
        model.addAttribute("cartLines", lines);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("orderTotal", subtotal.add(shippingFee));
        model.addAttribute("freeShippingThreshold", freeShippingThreshold);
        model.addAttribute("codEnabled", codEnabled);
    }

    private BigDecimal calculateShippingFee(BigDecimal subtotal) {
        if (freeShippingThreshold == null || freeShippingThreshold.signum() <= 0
                || shippingFlatFee == null || shippingFlatFee.signum() < 0) {
            throw new IllegalStateException("Chưa cấu hình đầy đủ phí vận chuyển.");
        }
        return subtotal.compareTo(freeShippingThreshold) < 0 ? shippingFlatFee : BigDecimal.ZERO;
    }

    public record CartLine(Product product, int quantity, BigDecimal lineTotal) {
    }
}
