package com.fitos.ai.chat;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    @EntityGraph(attributePaths = "messages")
    List<AiConversation> findByCustomerIdOrderByUpdatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = "messages")
    Optional<AiConversation> findWithMessagesById(Long id);
}
