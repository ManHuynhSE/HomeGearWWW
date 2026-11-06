package iuh.fit.demoprj.controller;

import iuh.fit.demoprj.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {
    private final CatalogService catalog;

    public HomeController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping({"/", "/products"})
    public String products(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("products", catalog.search(keyword));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "products";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("product", catalog.getProduct(id));
        return "product-detail";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}



