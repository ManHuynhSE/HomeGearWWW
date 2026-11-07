package iuh.fit.demoprj.repository;

import iuh.fit.demoprj.model.entity.*;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    boolean existsByCustomerId(Long customerId);
    Optional<CustomerOrder> findByCheckoutToken(String checkoutToken);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerOrder> findForCheckoutTokenByCheckoutToken(String checkoutToken);
    Optional<CustomerOrder> findByIdAndCustomerEmailIgnoreCase(Long id, String email);
}


