package com.fitos.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonBackReference
    private AiConversation conversation;

    @Column(nullable = false)
    private String role; // user, assistant, system

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "model_used")
    private String modelUsed;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

