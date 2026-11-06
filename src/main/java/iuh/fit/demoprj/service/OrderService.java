package iuh.fit.demoprj.service;

import iuh.fit.demoprj.model.entity.*;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.*;

import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.repository.UserAccountRepository;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final ProductRepository products;
    private final CustomerOrderRepository orders;
    private final UserAccountRepository accounts;

    public OrderService(ProductRepository products, CustomerOrderRepository orders,
                         UserAccountRepository accounts) {
        this.products = products;
        this.orders = orders;
        this.accounts = accounts;
    }

    @Transactional
    public CustomerOrder checkout(String email, Map<Long, Integer> cart) {
        if (cart == null || cart.isEmpty()) throw new IllegalArgumentException("Giỏ hàng đang trống.");
        UserAccount customer = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        List<Product> selected = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Số lượng sản phẩm không hợp lệ.");
            }
            Product product = products.findForCheckoutById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));
            if (product.getStock() < entry.getValue()) {
                throw new IllegalArgumentException("Sản phẩm " + product.getName()
                        + " chỉ còn " + product.getStock() + " sản phẩm, nhưng bạn đang đặt "
                        + entry.getValue() + ". Đơn hàng chưa được tạo; hãy giảm số lượng rồi thử lại.");
            }
            selected.add(product);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        CustomerOrder order = new CustomerOrder(customer, total);
        for (Product product : selected) {
            int quantity = cart.get(product.getId());
            product.setStock(product.getStock() - quantity);
            order.addItem(new OrderItem(product, quantity, product.getPrice()));
        }
        return orders.save(order);
    }

    public List<CustomerOrder> findByCustomer(String email) {
        UserAccount customer = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        return orders.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
    }

    public List<CustomerOrder> findAll() {
        return orders.findAll();
    }
}



