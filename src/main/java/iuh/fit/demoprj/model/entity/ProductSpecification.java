package iuh.fit.demoprj.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_specifications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_specification_key", columnNames = {"product_id", "spec_key"})
})
public class ProductSpecification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "spec_key", nullable = false, length = 100)
    private String specKey;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 200)
    private String value;

    @Column(length = 40)
    private String unit;

    @Column(nullable = false)
    private int displayOrder;

    protected ProductSpecification() {
    }

    public ProductSpecification(String specKey, String label, String value, String unit, int displayOrder) {
        this.specKey = specKey;
        this.label = label;
        this.value = value;
        this.unit = unit;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public String getSpecKey() { return specKey; }
    public String getLabel() { return label; }
    public String getValue() { return value; }
    public String getUnit() { return unit; }
    public int getDisplayOrder() { return displayOrder; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
