package iuh.fit.demoprj.controller;

import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.model.enums.OrderStatusLabels;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAttributes {
    private final CatalogService catalog;
    private final BigDecimal freeShippingThreshold;
    private final Environment environment;

    public GlobalModelAttributes(CatalogService catalog,
                                 @Value("${homegear.shipping.free-threshold:500000}") BigDecimal freeShippingThreshold,
                                 Environment environment) {
        this.catalog = catalog;
        this.freeShippingThreshold = freeShippingThreshold;
        this.environment = environment;
    }

    @ModelAttribute
    public void addCurrentUser(Model model, Authentication authentication, HttpServletRequest request) {
        model.addAttribute("isAdmin", false);
        model.addAttribute("orderStatusLabels", OrderStatusLabels.all());
        model.addAttribute("storeCategories", catalog.getCategories());
        model.addAttribute("cartCount", getCartCount(request));
        model.addAttribute("freeShippingThreshold", freeShippingThreshold);
        model.addAttribute("storeContact", Map.of(
                "hotline", property("homegear.store.contact.hotline"),
                "email", property("homegear.store.contact.email"),
                "address", property("homegear.store.contact.address")));
        model.addAttribute("storePolicies", Map.of(
                "shipping", property("homegear.store.policy.shipping-url"),
                "returns", property("homegear.store.policy.returns-url"),
                "warranty", property("homegear.store.policy.warranty-url"),
                "privacy", property("homegear.store.policy.privacy-url")));
        model.addAttribute("storeSocial", Map.of(
                "facebook", property("homegear.store.social.facebook-url"),
                "instagram", property("homegear.store.social.instagram-url"),
                "youtube", property("homegear.store.social.youtube-url")));
        model.addAttribute("storeChannels", Map.of(
                "zalo", property("homegear.store.channel.zalo-url"),
                "messenger", property("homegear.store.channel.messenger-url")));
        model.addAttribute("storePayments", Map.of(
                "cod", enabled("homegear.store.payment.cod-enabled"),
                "bankTransfer", enabled("homegear.store.payment.bank-transfer-enabled"),
                "vnpay", enabled("homegear.store.payment.vnpay-enabled"),
                "momo", enabled("homegear.store.payment.momo-enabled")));
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            model.addAttribute("currentUser", authentication);
            model.addAttribute("isAdmin", authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
        }
    }

    private long getCartCount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return 0;
        Object cart = session.getAttribute("cart");
        if (!(cart instanceof Map<?, ?> cartItems)) return 0;
        return cartItems.values().stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToLong(quantity -> Math.max(0, quantity.longValue()))
                .sum();
    }

    private String property(String key) {
        return environment.getProperty(key, "").trim();
    }

    private boolean enabled(String key) {
        return environment.getProperty(key, Boolean.class, false);
    }
}
