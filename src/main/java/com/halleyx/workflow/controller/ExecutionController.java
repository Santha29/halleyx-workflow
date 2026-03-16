package com.halleyx.workflow.controller;

import com.halleyx.workflow.dto.request.ExecutionRequest;
import com.halleyx.workflow.dto.response.ApiResponse;
import com.halleyx.workflow.dto.response.ExecutionResponse;
import com.halleyx.workflow.enums.ExecutionStatus;
import com.halleyx.workflow.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Executions", description = "Workflow execution APIs")
@CrossOrigin(origins = "*")
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping("/api/v1/workflows/{workflowId}/execute")
    @Operation(summary = "Execute a workflow with input data")
    public ResponseEntity<ApiResponse<ExecutionResponse>> execute(
            @PathVariable String workflowId,
            @RequestBody(required = false) ExecutionRequest request) {
        if (request == null) request = new ExecutionRequest();
        return ResponseEntity.ok(ApiResponse.success(
                executionService.execute(workflowId, request), "Workflow execution started"));
    }

    @GetMapping("/api/v1/executions")
    @Operation(summary = "List all executions with optional filters")
    public ResponseEntity<ApiResponse<Page<ExecutionResponse>>> getAll(
            @RequestParam(required = false) String workflowId,
            @RequestParam(required = false) ExecutionStatus status,
            @PageableDefault(size = 10, sort = "startedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(executionService.getAll(workflowId, status, pageable)));
    }

    @GetMapping("/api/v1/executions/{id}")
    @Operation(summary = "Get execution details with full step logs")
    public ResponseEntity<ApiResponse<ExecutionResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(executionService.getById(id)));
    }

    @PostMapping("/api/v1/executions/{id}/cancel")
    @Operation(summary = "Cancel a running execution")
    public ResponseEntity<ApiResponse<ExecutionResponse>> cancel(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(executionService.cancel(id), "Execution cancelled"));
    }

    @PostMapping("/api/v1/executions/{id}/retry")
    @Operation(summary = "Retry a failed execution")
    public ResponseEntity<ApiResponse<ExecutionResponse>> retry(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(executionService.retry(id), "Execution retried"));
    }
}
