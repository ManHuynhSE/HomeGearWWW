package iuh.fit.demoprj.model.entity;

import iuh.fit.demoprj.model.entity.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private UserAccount customer;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private String status = "NEW";

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {
    }

    public CustomerOrder(UserAccount customer, BigDecimal total) {
        this.customer = customer;
        this.total = total;
    }

    public Long getId() { return id; }
    public UserAccount getCustomer() { return customer; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public List<OrderItem> getItems() { return items; }
    public void addItem(OrderItem item) { items.add(item); item.setOrder(this); }
    public void setStatus(String status) { this.status = status; }
}

