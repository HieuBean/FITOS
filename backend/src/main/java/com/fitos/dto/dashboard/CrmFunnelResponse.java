package com.fitos.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmFunnelResponse {

    private List<FunnelStage> stages;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FunnelStage {
        private String stage;
        private long count;
        private double percentage;
    }
}
