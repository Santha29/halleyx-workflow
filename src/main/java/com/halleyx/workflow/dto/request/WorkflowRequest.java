package com.halleyx.workflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowRequest {
    @NotBlank(message = "Workflow name is required")
    private String name;
    private String description;
    private String inputSchema;
    private String startStepId;
    private Boolean isActive = true;
}
