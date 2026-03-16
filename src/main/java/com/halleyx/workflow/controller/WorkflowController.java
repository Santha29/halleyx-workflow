package com.halleyx.workflow.controller;

import com.halleyx.workflow.dto.request.WorkflowRequest;
import com.halleyx.workflow.dto.response.ApiResponse;
import com.halleyx.workflow.dto.response.WorkflowResponse;
import com.halleyx.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflows", description = "Workflow management APIs")
@CrossOrigin(origins = "*")
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    @Operation(summary = "Create a new workflow")
    public ResponseEntity<ApiResponse<WorkflowResponse>> create(@Valid @RequestBody WorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(workflowService.create(request), "Workflow created successfully"));
    }

    @GetMapping
    @Operation(summary = "List all workflows with search & pagination")
    public ResponseEntity<ApiResponse<Page<WorkflowResponse>>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.getAll(name, isActive, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow details with steps and rules")
    public ResponseEntity<ApiResponse<WorkflowResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update workflow (increments version)")
    public ResponseEntity<ApiResponse<WorkflowResponse>> update(
            @PathVariable String id, @Valid @RequestBody WorkflowRequest request) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.update(id, request), "Workflow updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a workflow")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        workflowService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Workflow deleted successfully"));
    }
}
