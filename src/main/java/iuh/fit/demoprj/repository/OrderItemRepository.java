package iuh.fit.demoprj.repository;

import iuh.fit.demoprj.model.entity.OrderItem;
import iuh.fit.demoprj.model.entity.Product;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByProductId(Long productId);

    @Query("""
            select item.product
            from OrderItem item
            where item.order.status <> 'CANCELLED'
              and item.product.active = true
            group by item.product
            order by sum(item.quantity) desc
            """)
    List<Product> findBestSellingProducts(Pageable pageable);

    @Query("""
            select item.product as product, sum(item.quantity) as unitsSold
            from OrderItem item
            where item.order.status <> 'CANCELLED'
              and item.order.createdAt >= :from
              and item.order.createdAt < :to
            group by item.product
            order by sum(item.quantity) desc, item.product.id desc
            """)
    List<ProductSalesProjection> findTopSellingProducts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("""
            select item.product.id as productId, sum(item.quantity) as purchasedQuantity
            from OrderItem item
            where item.order.status <> 'CANCELLED'
              and item.order.createdAt >= :from
              and item.order.createdAt < :to
            group by item.product.id
            """)
    List<ProductPurchaseProjection> sumPurchasedQuantityBetween(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
            select item.product
            from OrderItem item
            where item.order.status <> 'CANCELLED'
              and item.product.active = true
              and (:keyword is null or :keyword = ''
                   or lower(item.product.name) like lower(concat('%', :keyword, '%'))
                   or lower(item.product.description) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or item.product.category.id = :categoryId)
              and (:minPrice is null or item.product.price >= :minPrice)
              and (:maxPrice is null or item.product.price <= :maxPrice)
              and (:inStockOnly = false or item.product.stock > 0)
            group by item.product
            order by sum(item.quantity) desc, item.product.id desc
            """,
            countQuery = """
                    select count(distinct item.product.id)
                    from OrderItem item
                    where item.order.status <> 'CANCELLED'
                      and item.product.active = true
                      and (:keyword is null or :keyword = ''
                           or lower(item.product.name) like lower(concat('%', :keyword, '%'))
                           or lower(item.product.description) like lower(concat('%', :keyword, '%')))
                      and (:categoryId is null or item.product.category.id = :categoryId)
                      and (:minPrice is null or item.product.price >= :minPrice)
                      and (:maxPrice is null or item.product.price <= :maxPrice)
                      and (:inStockOnly = false or item.product.stock > 0)
                    """)
    Page<Product> findBestSellingCatalog(@Param("categoryId") Long categoryId,
                                         @Param("keyword") String keyword,
                                         @Param("minPrice") BigDecimal minPrice,
                                         @Param("maxPrice") BigDecimal maxPrice,
                                         @Param("inStockOnly") boolean inStockOnly,
                                         Pageable pageable);

    interface ProductSalesProjection {
        Product getProduct();
        Long getUnitsSold();
    }

    interface ProductPurchaseProjection {
        Long getProductId();
        Long getPurchasedQuantity();
    }
}
