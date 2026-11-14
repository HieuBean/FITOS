package com.fitos.repository;

import com.fitos.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndStudioId(Long id, Long studioId);

    Optional<Product> findByStudioIdAndBarcode(Long studioId, String barcode);

    Optional<Product> findByStudioIdAndSkuIgnoreCase(Long studioId, String sku);

    boolean existsByStudioIdAndSkuIgnoreCase(Long studioId, String sku);

    boolean existsByStudioIdAndBarcode(Long studioId, String barcode);

    boolean existsByStudioIdAndSkuIgnoreCaseAndIdNot(Long studioId, String sku, Long id);

    boolean existsByStudioIdAndBarcodeAndIdNot(Long studioId, String barcode, Long id);

    long countByStudioIdAndCategoryId(Long studioId, Long categoryId);

    /**
     * Khoá dòng sản phẩm khi bán/nhập kho — mirror
     * {@code ClassSessionRepository.findByIdWithLock} của BookingService, dùng để
     * chống bán vượt tồn khi nhiều thu ngân bán cùng lúc.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.studioId = :studioId")
    Optional<Product> findByIdWithLock(@Param("id") Long id, @Param("studioId") Long studioId);

    @Query("""
           SELECT p FROM Product p
           WHERE p.studioId = :studioId
             AND (:activeOnly = FALSE OR p.isActive = TRUE)
             AND (:categoryId IS NULL OR p.categoryId = :categoryId)
             AND (CAST(:q AS string) IS NULL
                  OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                  OR LOWER(p.sku) = LOWER(CAST(:q AS string))
                  OR p.barcode = CAST(:q AS string))
           ORDER BY p.name ASC
           """)
    Page<Product> search(@Param("studioId") Long studioId,
                         @Param("q") String q,
                         @Param("categoryId") Long categoryId,
                         @Param("activeOnly") boolean activeOnly,
                         Pageable pageable);

    @Query("""
           SELECT p FROM Product p
           WHERE p.studioId = :studioId AND p.isActive = TRUE AND p.trackInventory = TRUE
             AND p.stockQuantity <= p.lowStockThreshold
           ORDER BY (p.stockQuantity - p.lowStockThreshold) ASC, p.name ASC
           """)
    List<Product> findLowStock(@Param("studioId") Long studioId);

    List<Product> findByStudioIdAndIsActiveTrueOrderByNameAsc(Long studioId);
}
