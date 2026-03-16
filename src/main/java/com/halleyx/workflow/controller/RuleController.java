package com.halleyx.workflow.controller;

import com.halleyx.workflow.dto.request.RuleRequest;
import com.halleyx.workflow.dto.response.ApiResponse;
import com.halleyx.workflow.dto.response.RuleResponse;
import com.halleyx.workflow.entity.Step;
import com.halleyx.workflow.service.RuleService;
import com.halleyx.workflow.service.StepService;
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
@Tag(name = "Rules", description = "Rule management APIs")
@CrossOrigin(origins = "*")
public class RuleController {

    private final RuleService ruleService;
    private final StepService stepService;

    @PostMapping("/api/v1/steps/{stepId}/rules")
    @Operation(summary = "Add a rule to a step")
    public ResponseEntity<ApiResponse<RuleResponse>> create(
            @PathVariable String stepId, @Valid @RequestBody RuleRequest request) {
        Step step = stepService.findById(stepId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ruleService.create(step, request), "Rule created"));
    }

    @GetMapping("/api/v1/steps/{stepId}/rules")
    @Operation(summary = "List all rules for a step (ordered by priority)")
    public ResponseEntity<ApiResponse<List<RuleResponse>>> getByStep(@PathVariable String stepId) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getRulesByStep(stepId)));
    }

    @GetMapping("/api/v1/rules/{ruleId}")
    @Operation(summary = "Get rule by ID")
    public ResponseEntity<ApiResponse<RuleResponse>> getById(@PathVariable String ruleId) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getById(ruleId)));
    }

    @PutMapping("/api/v1/rules/{ruleId}")
    @Operation(summary = "Update a rule")
    public ResponseEntity<ApiResponse<RuleResponse>> update(
            @PathVariable String ruleId, @Valid @RequestBody RuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.update(ruleId, request), "Rule updated"));
    }

    @DeleteMapping("/api/v1/rules/{ruleId}")
    @Operation(summary = "Delete a rule")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String ruleId) {
        ruleService.delete(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null, "Rule deleted"));
    }
}
