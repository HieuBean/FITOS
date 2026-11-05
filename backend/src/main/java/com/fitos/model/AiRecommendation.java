package com.fitos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "studio_id", nullable = false)
    @Builder.Default
    private Long studioId = 1L;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Transient
    private Customer customer;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String type = "WORKOUT_CHANGE";

    @Column(nullable = false, length = 150)
    @Builder.Default
    private String title = "AI Recommendation";

    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String explanation = "Personalized recommendation";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggested_changes", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String suggestedChanges = "{}";

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @PrePersist
    public void prePersist() {
        if (customer != null) {
            this.customerId = customer.getId();
            if (customer.getStudioId() != null) {
                this.studioId = customer.getStudioId();
            }
        }
    }

    // Compatibility methods
    public void setCustomer(Customer customer) {
        this.customer = customer;
        if (customer != null) {
            this.customerId = customer.getId();
            if (customer.getStudioId() != null) {
                this.studioId = customer.getStudioId();
            }
        }
    }

    public void setRecommendation(String json) {
        this.suggestedChanges = json;
    }

    public String getRecommendation() {
        return suggestedChanges;
    }

    public void setRecType(String recType) {
        this.type = recType;
    }

    public void setService(String service) {
        // preserved for compatibility
    }

    public void setActedAt(LocalDateTime actedAt) {
        this.respondedAt = Instant.now();
    }
}
