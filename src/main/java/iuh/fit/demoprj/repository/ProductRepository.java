package iuh.fit.demoprj.repository;

import iuh.fit.demoprj.model.entity.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findForCheckoutByIdAndActiveTrue(Long id);

    @Query("""
            select distinct product
            from Product product
            left join fetch product.specifications
            where product.active = true
            """)
    List<Product> findAllActiveForVectorIndex();

    @Query("""
            select distinct product
            from Product product
            left join fetch product.specifications
            where product.category.id = :categoryId and product.active = true
            """)
    List<Product> findActiveForVectorIndexByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
            select distinct product
            from Product product
            left join fetch product.specifications
            where product.id = :id
            """)
    Optional<Product> findForVectorIndexById(@Param("id") Long id);

    @Query("""
            select distinct product
            from Product product
            left join fetch product.galleryImages
            where product.id = :id and product.active = true
            """)
    Optional<Product> findDetailById(@Param("id") Long id);

    List<Product> findTop4ByCategory_IdAndActiveTrueAndIdNotOrderByIdDesc(Long categoryId, Long id);
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description, Pageable pageable);
    @Query("""
            select product
            from Product product
            where product.active = true
              and (lower(product.name) like lower(concat('%', :keyword, '%'))
                or lower(product.description) like lower(concat('%', :keyword, '%')))
            """)
    List<Product> findActiveSuggestions(@Param("keyword") String keyword, Pageable pageable);
    List<Product> findTop6ByActiveTrueOrderByIdDesc();
    List<Product> findByActiveTrueAndOriginalPriceIsNotNullAndPromotionStartsAtLessThanEqualAndPromotionEndsAtGreaterThanOrderByPromotionEndsAtAsc(
            LocalDateTime start, LocalDateTime end);
    Optional<Product> findByIdAndActiveTrue(Long id);
    List<Product> findByIdInAndActiveTrue(List<Long> ids);
    boolean existsByNameIgnoreCaseAndCategoryId(String name, Long categoryId);
    boolean existsByNameIgnoreCaseAndCategoryIdAndIdNot(String name, Long categoryId, Long id);
    boolean existsByCategoryId(Long categoryId);
    long countByCategoryId(Long categoryId);
    List<Product> findTop20ByActiveTrueAndStockLessThanEqualOrderByStockAscIdAsc(int stock);

    @Query("""
            select product
            from Product product
            where product.active = true
              and (:keyword is null or :keyword = ''
                or lower(product.name) like lower(concat('%', :keyword, '%'))
                or lower(product.description) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or product.category.id = :categoryId)
              and (:minPrice is null or product.price >= :minPrice)
              and (:maxPrice is null or product.price <= :maxPrice)
              and (:inStockOnly = false or product.stock > 0)
            """)
    Page<Product> searchCatalog(@Param("keyword") String keyword,
                                @Param("categoryId") Long categoryId,
                                @Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice,
                                @Param("inStockOnly") boolean inStockOnly,
                                Pageable pageable);
}
