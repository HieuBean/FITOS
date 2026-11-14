package com.fitos.repository;

import com.fitos.model.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    Page<InventoryTransaction> findByStudioIdOrderByCreatedAtDesc(Long studioId, Pageable pageable);

    Page<InventoryTransaction> findByStudioIdAndProductIdOrderByCreatedAtDesc(
            Long studioId, Long productId, Pageable pageable);

    List<InventoryTransaction> findByStudioIdAndProductIdOrderByCreatedAtDesc(Long studioId, Long productId);

    boolean existsByStudioIdAndOrderIdAndProductIdAndTransactionType(
            Long studioId, Long orderId, Long productId, String transactionType);

    List<InventoryTransaction> findByStudioIdAndOrderId(Long studioId, Long orderId);
}
