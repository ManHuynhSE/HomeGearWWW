package iuh.fit.demoprj.service;

import iuh.fit.demoprj.model.entity.*;
import iuh.fit.demoprj.repository.*;
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
    public CustomerOrder checkout(String email, Map<Long, Integer> cart,
                                  String recipientName, String recipientPhone,
                                  String shippingAddress, BigDecimal freeShippingThreshold,
                                  BigDecimal flatShippingFee,
                                  String checkoutToken) {
        if (checkoutToken == null || checkoutToken.isBlank()) {
            throw new IllegalArgumentException("Phiên thanh toán không hợp lệ. Hãy thử lại.");
        }
        UserAccount customer = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        CustomerOrder existingOrder = orders.findByCheckoutToken(checkoutToken).orElse(null);
        if (existingOrder != null) {
            if (!existingOrder.getCustomer().getId().equals(customer.getId())) {
                throw new IllegalArgumentException("Phiên thanh toán không hợp lệ. Hãy thử lại.");
            }
            return existingOrder;
        }
        validateDeliveryDetails(recipientName, recipientPhone, shippingAddress,
                freeShippingThreshold, flatShippingFee);
        if (cart == null || cart.isEmpty()) throw new IllegalArgumentException("Giỏ hàng đang trống.");
        List<Product> selected = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        List<Map.Entry<Long, Integer>> orderedEntries = cart.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .toList();
        for (Map.Entry<Long, Integer> entry : orderedEntries) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Số lượng sản phẩm không hợp lệ.");
            }
            Product product = products.findForCheckoutById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));
            selected.add(product);
        }
        existingOrder = orders.findForCheckoutTokenByCheckoutToken(checkoutToken).orElse(null);
        if (existingOrder != null) {
            if (!existingOrder.getCustomer().getId().equals(customer.getId())) {
                throw new IllegalArgumentException("Phiên thanh toán không hợp lệ. Hãy thử lại.");
            }
            return existingOrder;
        }
        for (Product product : selected) {
            int quantity = cart.get(product.getId());
            if (product.getStock() < quantity) {
                throw new IllegalArgumentException("Sản phẩm " + product.getName()
                        + " chỉ còn " + product.getStock() + " sản phẩm, nhưng bạn đang đặt "
                        + quantity + ". Đơn hàng chưa được tạo; hãy giảm số lượng rồi thử lại.");
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        BigDecimal shippingFee = total.compareTo(freeShippingThreshold) < 0
                ? flatShippingFee : BigDecimal.ZERO;
        CustomerOrder order = new CustomerOrder(customer, total.add(shippingFee),
                recipientName.trim(), normalizePhone(recipientPhone), shippingAddress.trim(),
                "COD", shippingFee, checkoutToken);
        for (Product product : selected) {
            int quantity = cart.get(product.getId());
            product.setStock(product.getStock() - quantity);
            order.addItem(new OrderItem(product, quantity, product.getPrice()));
        }
        return orders.save(order);
    }

    private void validateDeliveryDetails(String recipientName, String recipientPhone,
                                         String shippingAddress, BigDecimal freeShippingThreshold,
                                         BigDecimal flatShippingFee) {
        if (recipientName == null || recipientName.isBlank() || recipientName.trim().length() > 120) {
            throw new IllegalArgumentException("Vui lòng nhập họ tên người nhận (tối đa 120 ký tự).");
        }
        String normalizedPhone = normalizePhone(recipientPhone);
        if (!normalizedPhone.matches("^(0\\d{9,10}|\\+84\\d{9,10})$")) {
            throw new IllegalArgumentException("Số điện thoại người nhận không hợp lệ.");
        }
        if (shippingAddress == null || shippingAddress.isBlank()
                || shippingAddress.trim().length() > 500) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ giao hàng (tối đa 500 ký tự).");
        }
        if (freeShippingThreshold == null || freeShippingThreshold.signum() <= 0
                || flatShippingFee == null || flatShippingFee.signum() < 0) {
            throw new IllegalArgumentException("Phí vận chuyển chưa được cấu hình hợp lệ.");
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s().-]", "");
    }

    public List<CustomerOrder> findByCustomer(String email) {
        UserAccount customer = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        return orders.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
    }

    public List<CustomerOrder> findAll() {
        return orders.findAll();
    }

    public CustomerOrder findOrderForCustomer(Long id, String email) {
        return orders.findByIdAndCustomerEmailIgnoreCase(id, email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
    }
}



