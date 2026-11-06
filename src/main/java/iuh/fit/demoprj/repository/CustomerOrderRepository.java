package iuh.fit.demoprj.repository;

import iuh.fit.demoprj.model.entity.*;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    boolean existsByCustomerId(Long customerId);
}


