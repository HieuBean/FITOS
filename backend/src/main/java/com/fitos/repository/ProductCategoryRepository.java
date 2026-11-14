package com.fitos.repository;

import com.fitos.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByStudioIdOrderBySortOrderAscNameAsc(Long studioId);

    List<ProductCategory> findByStudioIdAndIsActiveTrueOrderBySortOrderAscNameAsc(Long studioId);

    Optional<ProductCategory> findByIdAndStudioId(Long id, Long studioId);

    boolean existsByStudioIdAndNameIgnoreCase(Long studioId, String name);

    boolean existsByStudioIdAndNameIgnoreCaseAndIdNot(Long studioId, String name, Long id);
}
