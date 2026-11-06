package iuh.fit.demoprj.service;

import iuh.fit.demoprj.model.entity.CustomerOrder;
import iuh.fit.demoprj.model.entity.UserAccount;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.CustomerOrderRepository;
import iuh.fit.demoprj.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private static final List<String> ORDER_STATUSES =
            List.of("NEW", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELLED");

    private final UserAccountRepository accounts;
    private final CustomerOrderRepository orders;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserAccountRepository accounts, CustomerOrderRepository orders,
                        PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.orders = orders;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserAccount> findCustomers() {
        return accounts.findByRoleOrderByIdDesc(Role.CUSTOMER);
    }

    public List<UserAccount> searchCustomers(String keyword) {
        List<UserAccount> customers = findCustomers();
        if (keyword == null || keyword.isBlank()) return customers;
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return customers.stream()
                .filter(customer -> customer.getFullName().toLowerCase(Locale.ROOT).contains(normalized)
                        || customer.getEmail().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }

    public List<CustomerOrder> searchOrders(String keyword, String status) {
        List<CustomerOrder> allOrders = orders.findAll();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return allOrders.stream()
                .filter(order -> normalizedStatus.isBlank() || order.getStatus().equals(normalizedStatus))
                .filter(order -> normalizedKeyword.isBlank()
                        || order.getCustomer().getEmail().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || String.valueOf(order.getId()).equals(normalizedKeyword))
                .toList();
    }

    public UserAccount getCustomer(Long id) {
        UserAccount account = accounts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (account.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("Chỉ được quản lý tài khoản Customer.");
        }
        return account;
    }

    @Transactional
    public void updateCustomer(Long id, String fullName, String email, String password) {
        UserAccount account = getCustomer(id);
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (fullName == null || fullName.isBlank() || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Họ tên và email không được để trống.");
        }
        accounts.findByEmailIgnoreCase(normalizedEmail).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Email đã được sử dụng.");
            }
        });
        account.setFullName(fullName.trim());
        account.setEmail(normalizedEmail);
        if (password != null && !password.isBlank()) {
            if (password.length() < 8) {
                throw new IllegalArgumentException("Mật khẩu mới tối thiểu 8 ký tự.");
            }
            account.setPassword(passwordEncoder.encode(password));
        }
        accounts.save(account);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        getCustomer(id);
        if (orders.existsByCustomerId(id)) {
            throw new IllegalArgumentException("Tài khoản đã có đơn hàng, không thể xóa.");
        }
        accounts.deleteById(id);
    }

    public CustomerOrder getOrder(Long id) {
        return orders.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
    }

    @Transactional
    public void updateOrderStatus(Long id, String status) {
        if (!ORDER_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        }
        CustomerOrder order = getOrder(id);
        order.setStatus(status);
        orders.save(order);
    }

    public List<String> getOrderStatuses() {
        return ORDER_STATUSES;
    }

    public BigDecimal calculateRevenue(List<CustomerOrder> allOrders) {
        return allOrders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .map(CustomerOrder::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
