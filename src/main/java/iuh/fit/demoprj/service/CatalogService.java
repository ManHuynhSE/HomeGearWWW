package iuh.fit.demoprj.service;

import iuh.fit.demoprj.model.entity.*;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.*;

import java.util.List;
import java.time.LocalDateTime;
import java.net.URI;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final OrderItemRepository orderItems;

    public CatalogService(ProductRepository products, CategoryRepository categories,
                          OrderItemRepository orderItems) {
        this.products = products;
        this.categories = categories;
        this.orderItems = orderItems;
    }

    public List<Product> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return products.findAll();
        return products.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
    }

    public List<Product> searchSuggestions(String keyword) {
        return products.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                keyword, keyword, PageRequest.of(0, 7));
    }

    public Page<Product> searchCatalog(String keyword, Long categoryId, BigDecimal minPrice,
                                        BigDecimal maxPrice, boolean inStockOnly,
                                        String sortBy, int page, int pageSize) {
        if (minPrice != null && minPrice.signum() < 0
                || maxPrice != null && maxPrice.signum() < 0
                || minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Khoảng giá không hợp lệ.");
        }
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Sort sort = "bestSelling".equals(sortBy) ? Sort.unsorted() : catalogSort(sortBy);
        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize, sort);
        if ("bestSelling".equals(sortBy)) {
            return orderItems.findBestSellingCatalog(categoryId, normalizedKeyword,
                    minPrice, maxPrice, inStockOnly, pageable);
        }
        return products.searchCatalog(normalizedKeyword, categoryId, minPrice, maxPrice,
                inStockOnly, pageable);
    }

    private Sort catalogSort(String sortBy) {
        return switch (sortBy == null ? "" : sortBy) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }

    public List<Product> getNewArrivals() {
        return products.findTop6ByOrderByIdDesc();
    }

    public List<Product> getActivePromotions(LocalDateTime now) {
        return products.findByOriginalPriceIsNotNullAndPromotionStartsAtLessThanEqualAndPromotionEndsAtGreaterThanOrderByPromotionEndsAtAsc(
                now, now);
    }

    public Product getProduct(Long id) {
        return products.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
    }

    @Transactional(readOnly = true)
    public Product getProductDetail(Long id) {
        Product product = products.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        product.getSpecifications().size();
        return product;
    }

    @Transactional(readOnly = true)
    public String getProductSpecificationsText(Long id) {
        return getProduct(id).getSpecifications().stream()
                .map(specification -> specification.getLabel() + " | " + specification.getValue()
                        + " | " + (specification.getUnit() == null ? "" : specification.getUnit()))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    public List<Product> getRelatedProducts(Product product) {
        return products.findTop4ByCategory_IdAndIdNotOrderByIdDesc(
                product.getCategory().getId(), product.getId());
    }

    public List<Category> getCategories() {
        return categories.findAll();
    }

    public Category getCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));
    }

    @Transactional
    public Category saveCategory(Long id, String name) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        boolean duplicate = id == null
                ? categories.existsByNameIgnoreCase(normalizedName)
                : categories.existsByNameIgnoreCaseAndIdNot(normalizedName, id);
        if (duplicate) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại.");
        }
        Category category = id == null ? new Category(normalizedName) : getCategory(id);
        category.setName(normalizedName);
        return categories.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        getCategory(id);
        if (products.existsByCategoryId(id)) {
            throw new IllegalArgumentException("Danh mục đang có sản phẩm, không thể xóa.");
        }
        categories.deleteById(id);
    }

    @Transactional
    public Product saveProduct(Long id, String name, java.math.BigDecimal price, int stock,
                               java.math.BigDecimal originalPrice, LocalDateTime promotionStartsAt,
                               LocalDateTime promotionEndsAt, String description,
                               String imageUrl, String imageAttribution, String imageSourceUrl,
                               String specificationText, Long categoryId) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isBlank() || price == null || price.signum() <= 0 || stock < 0) {
            throw new IllegalArgumentException("Tên bắt buộc; giá phải lớn hơn 0; tồn kho không được âm.");
        }
        List<ProductSpecification> specifications = parseSpecifications(specificationText);
        if (originalPrice == null && (promotionStartsAt != null || promotionEndsAt != null)
                || originalPrice != null && (originalPrice.compareTo(price) <= 0
                || promotionStartsAt == null || promotionEndsAt == null
                || !promotionEndsAt.isAfter(promotionStartsAt))) {
            throw new IllegalArgumentException("Khuyến mãi cần giá gốc lớn hơn giá bán và thời gian bắt đầu/kết thúc hợp lệ.");
        }
        Category category = getCategory(categoryId);
        boolean duplicate = id == null
                ? products.existsByNameIgnoreCaseAndCategoryId(normalizedName, categoryId)
                : products.existsByNameIgnoreCaseAndCategoryIdAndIdNot(normalizedName, categoryId, id);
        if (duplicate) {
            throw new IllegalArgumentException("Tên sản phẩm đã tồn tại trong danh mục.");
        }
        Product product = id == null ? new Product(normalizedName, price, stock, description, category)
                : getProduct(id);
        product.setName(normalizedName);
        product.setPrice(price);
        product.setOriginalPrice(originalPrice);
        product.setPromotionStartsAt(promotionStartsAt);
        product.setPromotionEndsAt(promotionEndsAt);
        product.setStock(stock);
        product.setDescription(description == null ? "" : description.trim());
        product.setImageUrl(normalizeImageUrl(imageUrl));
        String normalizedAttribution = imageAttribution == null || imageAttribution.isBlank()
                ? null : imageAttribution.trim();
        String normalizedImageSourceUrl = normalizeImageUrl(imageSourceUrl);
        if (normalizedAttribution != null && normalizedImageSourceUrl == null) {
            throw new IllegalArgumentException("Nguồn ghi công ảnh cần có URL nguồn.");
        }
        product.setImageAttribution(normalizedAttribution);
        product.setImageSourceUrl(normalizedImageSourceUrl);
        product.setSpecifications(specifications);
        product.setCategory(category);
        return products.save(product);
    }

    private List<ProductSpecification> parseSpecifications(String text) {
        List<ProductSpecification> specifications = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        if (text == null || text.isBlank()) return specifications;
        String[] lines = text.split("\\R");
        if (lines.length > 30) {
            throw new IllegalArgumentException("Mỗi sản phẩm nhập tối đa 30 thông số.");
        }
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty()) continue;
            String[] fields = line.split("\\|", -1);
            if (fields.length != 3) {
                throw new IllegalArgumentException("Dòng " + (index + 1)
                        + " cần đúng định dạng: Tên | Giá trị | Đơn vị.");
            }
            String label = fields[0].trim();
            String value = fields[1].trim();
            String unit = fields[2].trim();
            if (label.isBlank() || value.isBlank() || label.length() > 100
                    || value.length() > 200 || unit.length() > 40) {
                throw new IllegalArgumentException("Dòng " + (index + 1)
                        + " có tên/giá trị trống hoặc vượt giới hạn ký tự.");
            }
            String key = Normalizer.normalize(label.replace('đ', 'd').replace('Đ', 'D'),
                            Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("(^-|-$)", "");
            if (key.isBlank() || !keys.add(key)) {
                throw new IllegalArgumentException("Tên thông số bị trùng hoặc không hợp lệ ở dòng "
                        + (index + 1) + ".");
            }
            specifications.add(new ProductSpecification(
                    key, label, value, unit.isBlank() ? null : unit, specifications.size()));
        }
        return specifications;
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        String normalized = imageUrl.trim();
        if (normalized.startsWith("/") && !normalized.startsWith("//")) return normalized;
        try {
            URI uri = URI.create(normalized);
            if (uri.isAbsolute() && "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
                return normalized;
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Ảnh sản phẩm phải là đường dẫn nội bộ hoặc URL HTTPS hợp lệ.");
        }
        throw new IllegalArgumentException("Ảnh sản phẩm phải là đường dẫn nội bộ hoặc URL HTTPS hợp lệ.");
    }

    @Transactional
    public void deleteProduct(Long id, iuh.fit.demoprj.repository.OrderItemRepository orderItems) {
        getProduct(id);
        if (orderItems.existsByProductId(id)) {
            throw new IllegalArgumentException("Sản phẩm đã có trong đơn hàng, không thể xóa.");
        }
        products.deleteById(id);
    }
}

