package com.halleyx.workflow.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RuleResponse {
    private String id;
    private String stepId;
    private String condition;
    private String nextStepId;
    private Integer priority;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
