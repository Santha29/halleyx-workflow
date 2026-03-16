package com.halleyx.workflow.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RuleRequest {
    @NotBlank(message = "Condition is required")
    private String condition;
    private String nextStepId;
    @NotNull(message = "Priority is required")
    @Min(value = 1, message = "Priority must be >= 1")
    private Integer priority;
    private String description;
}
