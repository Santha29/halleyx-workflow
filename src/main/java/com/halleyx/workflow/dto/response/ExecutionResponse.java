package com.halleyx.workflow.dto.response;

import com.halleyx.workflow.enums.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExecutionResponse {
    private String id;
    private String workflowId;
    private String workflowName;
    private Integer workflowVersion;
    private ExecutionStatus status;
    private Object inputData;
    private String currentStepId;
    private String currentStepName;
    private Integer retries;
    private String triggeredBy;
    private String errorMessage;
    private List<StepLogResponse> logs;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long durationSeconds;
}
