package iuh.fit.demoprj.controller;

import iuh.fit.demoprj.service.CatalogService;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
public class HomeController {
    private final CatalogService catalog;
    private final OrderItemRepository orderItems;

    public HomeController(CatalogService catalog, OrderItemRepository orderItems) {
        this.catalog = catalog;
        this.orderItems = orderItems;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Product> promotions = catalog.getActivePromotions(LocalDateTime.now());
        List<Product> arrivals = catalog.getNewArrivals();
        model.addAttribute("activePromotionIds", promotions.stream().map(Product::getId).toList());
        model.addAttribute("newArrivalIds", arrivals.stream().map(Product::getId).toList());
        model.addAttribute("promotionProducts", promotions);
        model.addAttribute("flashSaleProduct", promotions.isEmpty() ? null : promotions.get(0));
        model.addAttribute("bestSellingProducts", orderItems.findBestSellingProducts(PageRequest.of(0, 6)));
        model.addAttribute("newArrivals", arrivals);
        return "home";
    }

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) BigDecimal minPrice,
                           @RequestParam(required = false) BigDecimal maxPrice,
                           @RequestParam(defaultValue = "false") boolean inStockOnly,
                           @RequestParam(defaultValue = "newest") String sort,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        int pageSize = 12;
        int pageIndex = Math.max(0, page);
        String normalizedSort = Set.of("priceAsc", "priceDesc", "newest", "bestSelling").contains(sort)
                ? sort : "newest";
        Page<Product> catalogPage;
        try {
            catalogPage = catalog.searchCatalog(keyword, categoryId, minPrice, maxPrice,
                    inStockOnly, normalizedSort, pageIndex, pageSize);
            if (catalogPage.getTotalPages() > 0 && pageIndex >= catalogPage.getTotalPages()) {
                pageIndex = catalogPage.getTotalPages() - 1;
                catalogPage = catalog.searchCatalog(keyword, categoryId, minPrice, maxPrice,
                        inStockOnly, normalizedSort, pageIndex, pageSize);
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("filterError", ex.getMessage());
            catalogPage = Page.empty(PageRequest.of(0, pageSize, Sort.unsorted()));
            pageIndex = 0;
        }
        List<Product> arrivals = catalog.getNewArrivals();
        model.addAttribute("catalogPage", catalogPage);
        model.addAttribute("products", catalogPage.getContent());
        model.addAttribute("pageNumbers", pageNumbers(catalogPage));
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("inStockOnly", inStockOnly);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("page", pageIndex);
        model.addAttribute("newArrivalIds", arrivals.stream().map(Product::getId).toList());
        if (categoryId != null) model.addAttribute("selectedCategory", catalog.getCategory(categoryId));
        LocalDateTime now = LocalDateTime.now();
        List<Product> promotions = catalog.getActivePromotions(now);
        model.addAttribute("activePromotionIds", promotions.stream().map(Product::getId).toList());
        return "products";
    }

    @GetMapping("/products/suggestions")
    @ResponseBody
    public List<ProductSuggestion> suggestions(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().length() < 2) return List.of();
        return catalog.searchSuggestions(keyword.trim()).stream()
                .map(product -> new ProductSuggestion(
                        product.getId(), product.getName(), product.getPrice(), product.getStock()))
                .toList();
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = catalog.getProduct(id);
        LocalDateTime now = LocalDateTime.now();
        model.addAttribute("product", product);
        model.addAttribute("promotionActive", product.getOriginalPrice() != null
                && product.getPromotionStartsAt() != null
                && !product.getPromotionStartsAt().isAfter(now)
                && product.getPromotionEndsAt() != null
                && product.getPromotionEndsAt().isAfter(now));
        return "product-detail";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    public record ProductSuggestion(Long id, String name, BigDecimal price, int stock) {
    }

    private List<Integer> pageNumbers(Page<?> page) {
        int totalPages = page.getTotalPages();
        if (totalPages == 0) return List.of();
        int start = Math.max(0, page.getNumber() - 2);
        int end = Math.min(totalPages - 1, start + 4);
        start = Math.max(0, end - 4);
        return IntStream.rangeClosed(start, end).boxed().toList();
    }
}



