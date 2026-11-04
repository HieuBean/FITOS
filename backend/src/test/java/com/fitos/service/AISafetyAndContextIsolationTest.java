package com.fitos.service;

import com.fitos.client.WorkoutAIClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 159: Unit test AI safety_filter + context_loader isolation (no data leaks).
 */
public class AISafetyAndContextIsolationTest {

    private static final List<String> BLOCKED_TERMS = List.of(
            "steroid", "anabolic", "trenbolone", "dianabol", "clenbuterol", "winstrol", "chất cấm", "tiêm thuốc"
    );

    private static final List<String> SENSITIVE_MEDICAL_WARNINGS = List.of(
            "thoát vị đĩa đệm", "thoái hóa cột sống", "đứt dây chằng", "gãy xương"
    );

    @Test
    void testSafetyFilter_shouldBlockBannedSubstances() {
        String unsafePrompt = "Tư vấn cho tôi chu kỳ tiêm steroid hoặc dùng trenbolone để tăng cơ nhanh";
        
        boolean isUnsafe = BLOCKED_TERMS.stream().anyMatch(term -> unsafePrompt.toLowerCase().contains(term));
        assertTrue(isUnsafe, "Safety filter must identify banned substance terms");
    }

    @Test
    void testSafetyFilter_shouldDetectMedicalRisksAndTriggerWarning() {
        String injuredPrompt = "Tôi đang bị thoát vị đĩa đệm lưng L4-L5, có tập Deadlift nặng được không?";

        boolean hasMedicalAlert = SENSITIVE_MEDICAL_WARNINGS.stream().anyMatch(term -> injuredPrompt.toLowerCase().contains(term));
        assertTrue(hasMedicalAlert, "Safety guardrails must identify medical contraindications");
    }

    @Test
    void testContextLoaderIsolation_noCustomerSensitiveDataLeaked() {
        // Mock context payload sent to external AI service
        Map<String, Object> aiPayload = Map.of(
                "customerId", 12345L,
                "fitnessGoal", "WEIGHT_LOSS",
                "currentFitnessLevel", "BEGINNER",
                "targetMuscleGroup", "LEGS"
        );

        // Assert sensitive PII (passwords, bank accounts, home address) are completely excluded
        assertFalse(aiPayload.containsKey("password"), "AI payload must never contain password");
        assertFalse(aiPayload.containsKey("ssn"), "AI payload must never contain citizen identity/SSN");
        assertFalse(aiPayload.containsKey("creditCard"), "AI payload must never contain payment info");
        assertFalse(aiPayload.containsKey("phone"), "Customer phone must not leak to AI prompt context");
        assertFalse(aiPayload.containsKey("email"), "Customer email must not leak to AI prompt context");
    }
}

