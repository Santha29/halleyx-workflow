package com.halleyx.workflow.controller;

import com.halleyx.workflow.dto.request.StepRequest;
import com.halleyx.workflow.dto.response.ApiResponse;
import com.halleyx.workflow.dto.response.StepResponse;
import com.halleyx.workflow.entity.Workflow;
import com.halleyx.workflow.service.StepService;
import com.halleyx.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Steps", description = "Step management APIs")
@CrossOrigin(origins = "*")
public class StepController {

    private final StepService stepService;
    private final WorkflowService workflowService;

    @PostMapping("/api/v1/workflows/{workflowId}/steps")
    @Operation(summary = "Add a step to a workflow")
    public ResponseEntity<ApiResponse<StepResponse>> create(
            @PathVariable String workflowId, @Valid @RequestBody StepRequest request) {
        Workflow workflow = workflowService.findById(workflowId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(stepService.create(workflow, request), "Step created"));
    }

    @GetMapping("/api/v1/workflows/{workflowId}/steps")
    @Operation(summary = "List all steps for a workflow")
    public ResponseEntity<ApiResponse<List<StepResponse>>> getByWorkflow(@PathVariable String workflowId) {
        return ResponseEntity.ok(ApiResponse.success(stepService.getStepsByWorkflow(workflowId)));
    }

    @GetMapping("/api/v1/steps/{stepId}")
    @Operation(summary = "Get step details with rules")
    public ResponseEntity<ApiResponse<StepResponse>> getById(@PathVariable String stepId) {
        return ResponseEntity.ok(ApiResponse.success(stepService.getById(stepId)));
    }

    @PutMapping("/api/v1/steps/{stepId}")
    @Operation(summary = "Update a step")
    public ResponseEntity<ApiResponse<StepResponse>> update(
            @PathVariable String stepId, @Valid @RequestBody StepRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stepService.update(stepId, request), "Step updated"));
    }

    @DeleteMapping("/api/v1/steps/{stepId}")
    @Operation(summary = "Delete a step")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String stepId) {
        stepService.delete(stepId);
        return ResponseEntity.ok(ApiResponse.success(null, "Step deleted"));
    }
}
