package com.fitos.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadScoringResponse {
    private int score; // 0-100
    private String persona;
    private String recommendedAction;
}
