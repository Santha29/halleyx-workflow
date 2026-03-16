package com.halleyx.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halleyx.workflow.dto.request.ExecutionRequest;
import com.halleyx.workflow.dto.response.ExecutionResponse;
import com.halleyx.workflow.dto.response.StepLogResponse;
import com.halleyx.workflow.engine.RuleEngine;
import com.halleyx.workflow.entity.*;
import com.halleyx.workflow.enums.ExecutionStatus;
import com.halleyx.workflow.enums.StepStatus;
import com.halleyx.workflow.exception.ResourceNotFoundException;
import com.halleyx.workflow.exception.WorkflowExecutionException;
import com.halleyx.workflow.repository.ExecutionRepository;
import com.halleyx.workflow.repository.RuleRepository;
import com.halleyx.workflow.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);
    private static final int MAX_LOOP_ITERATIONS = 50;

    private final ExecutionRepository executionRepository;
    private final StepRepository stepRepository;
    private final RuleRepository ruleRepository;
    private final WorkflowService workflowService;
    private final RuleEngine ruleEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExecutionResponse execute(String workflowId, ExecutionRequest request) {
        Workflow workflow = workflowService.findById(workflowId);

        if (!Boolean.TRUE.equals(workflow.getIsActive())) {
            throw new WorkflowExecutionException("Workflow '" + workflow.getName() + "' is not active");
        }

        // Parse input data
        Map<String, Object> inputData = request.getInputData() != null ? request.getInputData() : new HashMap<>();

        // Validate input schema if defined
        validateInputSchema(workflow, inputData);

        // Create execution record
        Execution execution = Execution.builder()
                .workflowId(workflow.getId())
                .workflowName(workflow.getName())
                .workflowVersion(workflow.getVersion())
                .status(ExecutionStatus.IN_PROGRESS)
                .inputData(toJson(inputData))
                .triggeredBy(request.getTriggeredBy() != null ? request.getTriggeredBy() : "system")
                .retries(0)
                .build();
        execution = executionRepository.save(execution);

        log.info("Starting execution [id={}] for workflow '{}' (v{})",
                execution.getId(), workflow.getName(), workflow.getVersion());

        try {
            runWorkflow(execution, workflow, inputData);
        } catch (Exception e) {
            log.error("Execution [id={}] failed: {}", execution.getId(), e.getMessage());
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setEndedAt(LocalDateTime.now());
            executionRepository.save(execution);
        }

        return toResponse(executionRepository.findById(execution.getId()).orElse(execution));
    }

    private void runWorkflow(Execution execution, Workflow workflow, Map<String, Object> inputData) {
        List<Step> allSteps = stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflow.getId());
        if (allSteps.isEmpty()) {
            throw new WorkflowExecutionException("Workflow has no steps defined");
        }

        // Build step map for quick lookup
        Map<String, Step> stepMap = allSteps.stream().collect(Collectors.toMap(Step::getId, s -> s));

        // Find starting step
        Step currentStep = workflow.getStartStepId() != null && stepMap.containsKey(workflow.getStartStepId())
                ? stepMap.get(workflow.getStartStepId())
                : allSteps.get(0);

        int iterations = 0;

        while (currentStep != null) {
            if (iterations >= MAX_LOOP_ITERATIONS) {
                throw new WorkflowExecutionException(
                        "Max loop iterations (" + MAX_LOOP_ITERATIONS + ") reached. Possible infinite loop detected.");
            }
            iterations++;

            long stepStart = System.currentTimeMillis();
            log.info("Executing step [{}] '{}' (type={})", currentStep.getId(), currentStep.getName(), currentStep.getStepType());

            execution.setCurrentStepId(currentStep.getId());
            execution.setCurrentStepName(currentStep.getName());
            executionRepository.save(execution);

            // Execute step based on type
            StepLog stepLog = executeStep(execution, currentStep, inputData, stepStart);

            // Determine next step from rule evaluation
            String nextStepId = stepLog.getNextStepId();
            currentStep = (nextStepId != null && stepMap.containsKey(nextStepId))
                    ? stepMap.get(nextStepId)
                    : null;
        }

        // Mark execution complete
        execution.setStatus(ExecutionStatus.COMPLETED);
        execution.setCurrentStepId(null);
        execution.setCurrentStepName(null);
        execution.setEndedAt(LocalDateTime.now());
        executionRepository.save(execution);
        log.info("Execution [id={}] COMPLETED after {} steps", execution.getId(), iterations);
    }

    private StepLog executeStep(Execution execution, Step step, Map<String, Object> inputData, long stepStart) {
        StepLog.StepLogBuilder logBuilder = StepLog.builder()
                .execution(execution)
                .stepId(step.getId())
                .stepName(step.getName())
                .stepType(step.getStepType().name());

        try {
            // Execute step type specific logic
            switch (step.getStepType()) {
                case APPROVAL -> handleApprovalStep(step, logBuilder, inputData);
                case NOTIFICATION -> handleNotificationStep(step, logBuilder);
                case TASK -> handleTaskStep(step, logBuilder);
            }

            // Evaluate rules to get next step
            List<Rule> rules = ruleRepository.findByStepIdOrderByPriorityAsc(step.getId());
            RuleEngine.RuleEvaluationResult result = ruleEngine.evaluate(rules, inputData);

            logBuilder.status(StepStatus.COMPLETED)
                    .ruleEvaluated(result.getCondition())
                    .nextStepId(result.getNextStepId())
                    .message(result.getMessage())
                    .durationMs(System.currentTimeMillis() - stepStart);

            // Resolve next step name if available
            if (result.getNextStepId() != null) {
                stepRepository.findById(result.getNextStepId())
                        .ifPresent(ns -> logBuilder.nextStepName(ns.getName()));
            }

        } catch (Exception e) {
            log.error("Step '{}' execution failed: {}", step.getName(), e.getMessage());
            logBuilder.status(StepStatus.FAILED)
                    .message("Step failed: " + e.getMessage())
                    .durationMs(System.currentTimeMillis() - stepStart);
        }

        StepLog stepLog = logBuilder.build();
        execution.getLogs().add(stepLog);
        executionRepository.save(execution);
        return stepLog;
    }

    private void handleApprovalStep(Step step, StepLog.StepLogBuilder logBuilder, Map<String, Object> inputData) {
        String approverEmail = extractFromMetadata(step.getMetadata(), "assignee_email");
        logBuilder.approverEmail(approverEmail);
        logBuilder.message("Approval step processed. Approver: " + (approverEmail != null ? approverEmail : "N/A"));
        log.info("APPROVAL step '{}' - Approver: {}", step.getName(), approverEmail);
    }

    private void handleNotificationStep(Step step, StepLog.StepLogBuilder logBuilder) {
        String channel = extractFromMetadata(step.getMetadata(), "notification_channel");
        String template = extractFromMetadata(step.getMetadata(), "template");
        String message = "Notification sent via " + (channel != null ? channel : "default") +
                (template != null ? ". Template: " + template : "");
        logBuilder.message(message);
        log.info("NOTIFICATION step '{}' - Channel: {}, Template: {}", step.getName(), channel, template);
    }

    private void handleTaskStep(Step step, StepLog.StepLogBuilder logBuilder) {
        String instructions = extractFromMetadata(step.getMetadata(), "instructions");
        logBuilder.message("Task executed. " + (instructions != null ? "Instructions: " + instructions : ""));
        log.info("TASK step '{}' executed", step.getName());
    }

    private String extractFromMetadata(String metadataJson, String key) {
        if (metadataJson == null || metadataJson.isBlank()) return null;
        try {
            Map<String, Object> meta = objectMapper.readValue(metadataJson, new TypeReference<>() {});
            Object val = meta.get(key);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void validateInputSchema(Workflow workflow, Map<String, Object> inputData) {
        if (workflow.getInputSchema() == null || workflow.getInputSchema().isBlank()) return;
        try {
            Map<String, Object> schema = objectMapper.readValue(workflow.getInputSchema(), new TypeReference<>() {});
            Object required = schema.get("required");
            if (required instanceof List<?> requiredFields) {
                for (Object field : requiredFields) {
                    if (!inputData.containsKey(field.toString())) {
                        throw new WorkflowExecutionException("Missing required input field: " + field);
                    }
                }
            }
        } catch (WorkflowExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not validate input schema: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ExecutionResponse getById(String executionId) {
        return toResponse(findById(executionId));
    }

    @Transactional(readOnly = true)
    public Page<ExecutionResponse> getAll(String workflowId, ExecutionStatus status, Pageable pageable) {
        if (workflowId != null) return executionRepository.findByWorkflowId(workflowId, pageable).map(this::toResponse);
        if (status != null) return executionRepository.findByStatus(status, pageable).map(this::toResponse);
        return executionRepository.findAll(pageable).map(this::toResponse);
    }

    public ExecutionResponse cancel(String executionId) {
        Execution execution = findById(executionId);
        if (execution.getStatus() != ExecutionStatus.IN_PROGRESS && execution.getStatus() != ExecutionStatus.PENDING) {
            throw new WorkflowExecutionException("Cannot cancel execution in status: " + execution.getStatus());
        }
        execution.setStatus(ExecutionStatus.CANCELED);
        execution.setEndedAt(LocalDateTime.now());
        execution.setErrorMessage("Execution cancelled by user");
        return toResponse(executionRepository.save(execution));
    }

    public ExecutionResponse retry(String executionId) {
        Execution execution = findById(executionId);
        if (execution.getStatus() != ExecutionStatus.FAILED) {
            throw new WorkflowExecutionException("Only FAILED executions can be retried");
        }
        execution.setStatus(ExecutionStatus.IN_PROGRESS);
        execution.setRetries(execution.getRetries() + 1);
        execution.setEndedAt(null);
        execution.setErrorMessage(null);
        execution = executionRepository.save(execution);

        Workflow workflow = workflowService.findById(execution.getWorkflowId());
        Map<String, Object> inputData;
        try {
            inputData = objectMapper.readValue(execution.getInputData(), new TypeReference<>() {});
        } catch (Exception e) {
            inputData = new HashMap<>();
        }

        try {
            runWorkflow(execution, workflow, inputData);
        } catch (Exception e) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setEndedAt(LocalDateTime.now());
            executionRepository.save(execution);
        }

        return toResponse(executionRepository.findById(executionId).orElse(execution));
    }

    public Execution findById(String id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Execution", id));
    }

    private ExecutionResponse toResponse(Execution execution) {
        List<StepLogResponse> logs = execution.getLogs().stream().map(l ->
                StepLogResponse.builder()
                        .id(l.getId())
                        .stepId(l.getStepId())
                        .stepName(l.getStepName())
                        .stepType(l.getStepType())
                        .status(l.getStatus())
                        .ruleEvaluated(l.getRuleEvaluated())
                        .nextStepId(l.getNextStepId())
                        .nextStepName(l.getNextStepName())
                        .message(l.getMessage())
                        .approverEmail(l.getApproverEmail())
                        .durationMs(l.getDurationMs())
                        .executedAt(l.getExecutedAt())
                        .build()
        ).collect(Collectors.toList());

        Object inputDataObj = null;
        try {
            if (execution.getInputData() != null)
                inputDataObj = objectMapper.readValue(execution.getInputData(), Object.class);
        } catch (Exception ignored) {}

        Long durationSec = null;
        if (execution.getStartedAt() != null && execution.getEndedAt() != null) {
            durationSec = java.time.Duration.between(execution.getStartedAt(), execution.getEndedAt()).getSeconds();
        }

        return ExecutionResponse.builder()
                .id(execution.getId())
                .workflowId(execution.getWorkflowId())
                .workflowName(execution.getWorkflowName())
                .workflowVersion(execution.getWorkflowVersion())
                .status(execution.getStatus())
                .inputData(inputDataObj)
                .currentStepId(execution.getCurrentStepId())
                .currentStepName(execution.getCurrentStepName())
                .retries(execution.getRetries())
                .triggeredBy(execution.getTriggeredBy())
                .errorMessage(execution.getErrorMessage())
                .logs(logs)
                .startedAt(execution.getStartedAt())
                .endedAt(execution.getEndedAt())
                .durationSeconds(durationSec)
                .build();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }
}
