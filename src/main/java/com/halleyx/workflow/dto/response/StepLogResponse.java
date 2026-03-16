package com.halleyx.workflow.dto.response;

import com.halleyx.workflow.enums.StepStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class StepLogResponse {
    private String id;
    private String stepId;
    private String stepName;
    private String stepType;
    private StepStatus status;
    private String ruleEvaluated;
    private String nextStepId;
    private String nextStepName;
    private String message;
    private String approverEmail;
    private Long durationMs;
    private LocalDateTime executedAt;
}
