package iuh.fit.demoprj.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

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
    public Category getCategory() { return category; }
    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public void setPromotionStartsAt(LocalDateTime promotionStartsAt) { this.promotionStartsAt = promotionStartsAt; }
    public void setPromotionEndsAt(LocalDateTime promotionEndsAt) { this.promotionEndsAt = promotionEndsAt; }
    public void setStock(int stock) { this.stock = stock; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCategory(Category category) { this.category = category; }
}


