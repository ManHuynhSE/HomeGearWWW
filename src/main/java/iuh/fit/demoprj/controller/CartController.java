package iuh.fit.demoprj.controller;

import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.service.OrderService;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CartController {
    private static final String CART = "cart";
    private final CatalogService catalog;
    private final OrderService orders;

    public CartController(CatalogService catalog, OrderService orders) {
        this.catalog = catalog;
        this.orders = orders;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> cart(HttpSession session) {
        return (Map<Long, Integer>) session.getAttribute(CART);
    }

    @PostMapping("/cart/add/{id}")
    public String add(@PathVariable Long id, HttpSession session) {
        var product = catalog.getProduct(id);
        Map<Long, Integer> cart = cart(session);
        if (cart == null) cart = new LinkedHashMap<>();
        int next = cart.getOrDefault(id, 0) + 1;
        if (next > product.getStock()) throw new IllegalArgumentException("Số lượng vượt tồn kho.");
        cart.put(id, next);
        session.setAttribute(CART, cart);
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String view(HttpSession session, Model model) {
        Map<Long, Integer> cart = cart(session);
        Map<Long, Object> items = new LinkedHashMap<>();
        if (cart != null) {
            cart.forEach((id, quantity) -> items.put(id, catalog.getProduct(id)));
        }
        model.addAttribute("items", items);
        model.addAttribute("quantities", cart == null ? Map.of() : cart);
        return "cart";
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam Long productId, @RequestParam int quantity,
                         HttpSession session, Model model) {
        Map<Long, Integer> cart = cart(session);
        try {
            if (cart != null) {
                var product = catalog.getProduct(productId);
                if (quantity <= 0) cart.remove(productId);
                else if (quantity <= product.getStock()) cart.put(productId, quantity);
                else throw new IllegalArgumentException("Sản phẩm " + product.getName()
                        + " chỉ còn " + product.getStock() + " sản phẩm. "
                        + "Số lượng trong giỏ chưa thay đổi.");
                session.setAttribute(CART, cart);
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return view(session, model);
        }
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(HttpSession session, Authentication authentication, Model model) {
        Map<Long, Integer> cart = cart(session);
        try {
            orders.checkout(authentication.getName(), cart);
            session.removeAttribute(CART);
            return "redirect:/orders?success";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return view(session, model);
        }
    }
}


