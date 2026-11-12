package com.fitos.client;

import com.fitos.client.dto.LeadScoringRequest;
import com.fitos.client.dto.LeadScoringResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class LeadAIClient {

    private final RestClient restClient;
    private final String aiToken;

    public LeadAIClient(@Value("${fitos.ai.crm-url:http://localhost:8001}") String crmAiUrl,
                         @Value("${fitos.ai.token:fitos-ai-token}") String aiToken) {
        this.aiToken = aiToken;
        this.restClient = RestClient.builder()
                .baseUrl(crmAiUrl)
                .build();
    }

    public LeadScoringResponse scoreLead(LeadScoringRequest request) {
        try {
            return restClient.post()
                    .uri("/crm/score")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + aiToken)
                    .body(request)
                    .retrieve()
                    .body(LeadScoringResponse.class);
        } catch (Exception ex) {
            log.warn("Lead AI service unavailable: {}. Using heuristic scoring fallback.", ex.getMessage());
            return calculateHeuristicFallback(request);
        }
    }

    private LeadScoringResponse calculateHeuristicFallback(LeadScoringRequest req) {
        int baseScore = 40;
        if ("TRIAL".equalsIgnoreCase(req.getStatus())) baseScore += 30;
        else if ("INTERESTED".equalsIgnoreCase(req.getStatus())) baseScore += 20;
        else if ("CONTACTED".equalsIgnoreCase(req.getStatus())) baseScore += 10;

        baseScore += Math.min(req.getActivitiesCount() * 5, 25);
        int finalScore = Math.min(Math.max(baseScore, 10), 95);

        String persona = "Warm Prospect (" + (req.getGoal() != null ? req.getGoal() : "Fitness") + ")";
        String action = finalScore >= 70 ? "High priority: Call for Trial" : "Nurture with messages";

        return LeadScoringResponse.builder()
                .score(finalScore)
                .persona(persona)
                .recommendedAction(action)
                .build();
    }
}
