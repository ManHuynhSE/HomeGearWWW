package iuh.fit.demoprj.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(length = 255)
    private String imageAttribution;

    @Column(length = 1000)
    private String imageSourceUrl;

    protected ProductImage() {
    }

    public ProductImage(String imageUrl, String imageAttribution, String imageSourceUrl) {
        this.imageUrl = imageUrl;
        this.imageAttribution = imageAttribution;
        this.imageSourceUrl = imageSourceUrl;
    }

    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getImageAttribution() { return imageAttribution; }
    public String getImageSourceUrl() { return imageSourceUrl; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
