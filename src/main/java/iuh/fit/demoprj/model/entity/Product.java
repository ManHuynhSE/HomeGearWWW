package iuh.fit.demoprj.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(precision = 15, scale = 2)
    private BigDecimal originalPrice;

    private LocalDateTime promotionStartsAt;

    private LocalDateTime promotionEndsAt;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, length = 2000)
    private String description = "";

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 255)
    private String imageAttribution;

    @Column(length = 1000)
    private String imageSourceUrl;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ProductImage> galleryImages = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<ProductSpecification> specifications = new ArrayList<>();

    protected Product() {
    }

    public Product(String name, BigDecimal price, int stock, String description, Category category) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public int getDiscountPercent() {
        if (originalPrice == null || originalPrice.signum() <= 0 || originalPrice.compareTo(price) <= 0) return 0;
        return originalPrice.subtract(price).multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, RoundingMode.HALF_UP).intValue();
    }
    public LocalDateTime getPromotionStartsAt() { return promotionStartsAt; }
    public LocalDateTime getPromotionEndsAt() { return promotionEndsAt; }
    public int getStock() { return stock; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getImageAttribution() { return imageAttribution; }
    public String getImageSourceUrl() { return imageSourceUrl; }
    public Category getCategory() { return category; }
    public List<ProductImage> getGalleryImages() { return galleryImages; }
    public List<ProductSpecification> getSpecifications() { return specifications; }
    public String getSku() {
        return id == null ? "" : String.format(Locale.ROOT, "HG-%06d", id);
    }
    public String getSlug() {
        String normalized = Normalizer.normalize(name.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "san-pham" : normalized;
    }
    public String getCanonicalPath() {
        return id == null ? "/products" : "/san-pham/" + getSlug() + "-" + id;
    }
    public void addGalleryImage(ProductImage image) {
        image.setProduct(this);
        galleryImages.add(image);
    }
    public void setSpecifications(List<ProductSpecification> specifications) {
        this.specifications.clear();
        if (specifications != null) {
            specifications.forEach(specification -> {
                specification.setProduct(this);
                this.specifications.add(specification);
            });
        }
    }
    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public void setPromotionStartsAt(LocalDateTime promotionStartsAt) { this.promotionStartsAt = promotionStartsAt; }
    public void setPromotionEndsAt(LocalDateTime promotionEndsAt) { this.promotionEndsAt = promotionEndsAt; }
    public void setStock(int stock) { this.stock = stock; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setImageAttribution(String imageAttribution) { this.imageAttribution = imageAttribution; }
    public void setImageSourceUrl(String imageSourceUrl) { this.imageSourceUrl = imageSourceUrl; }
    public void setCategory(Category category) { this.category = category; }
}


