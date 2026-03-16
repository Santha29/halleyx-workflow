package com.halleyx.workflow.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class ExecutionRequest {
    private Map<String, Object> inputData;
    private String triggeredBy;
}
