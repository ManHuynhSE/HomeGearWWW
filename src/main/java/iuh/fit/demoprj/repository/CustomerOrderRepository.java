package iuh.fit.demoprj.repository;

import iuh.fit.demoprj.model.entity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<CustomerOrder> findTop10ByCustomerIdOrderByCreatedAtDesc(Long customerId);
    @Query("""
            select coalesce(sum(customerOrder.total), 0)
            from CustomerOrder customerOrder
            where customerOrder.status = 'COMPLETED'
              and customerOrder.createdAt >= :from
              and customerOrder.createdAt < :to
            """)
    BigDecimal sumCompletedRevenueBetween(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Query("""
            select customerOrder.status as status,
                   count(customerOrder.id) as orderCount,
                   coalesce(sum(customerOrder.total), 0) as total
            from CustomerOrder customerOrder
            where customerOrder.createdAt >= :from
              and customerOrder.createdAt < :to
            group by customerOrder.status
            """)
    List<OrderStatusAggregate> summarizeOrdersBetween(@Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);
    boolean existsByCustomerId(Long customerId);
    Optional<CustomerOrder> findByCheckoutToken(String checkoutToken);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerOrder> findForCheckoutTokenByCheckoutToken(String checkoutToken);
    Optional<CustomerOrder> findByIdAndCustomerEmailIgnoreCase(Long id, String email);

    interface OrderStatusAggregate {
        String getStatus();
        Long getOrderCount();
        BigDecimal getTotal();
    }
}


