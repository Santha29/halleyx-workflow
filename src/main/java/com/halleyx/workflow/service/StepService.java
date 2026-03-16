package com.halleyx.workflow.service;

import com.halleyx.workflow.dto.request.StepRequest;
import com.halleyx.workflow.dto.response.RuleResponse;
import com.halleyx.workflow.dto.response.StepResponse;
import com.halleyx.workflow.entity.Step;
import com.halleyx.workflow.entity.Workflow;
import com.halleyx.workflow.exception.ResourceNotFoundException;
import com.halleyx.workflow.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StepService {

    private static final Logger log = LoggerFactory.getLogger(StepService.class);
    private final StepRepository stepRepository;
    private final RuleService ruleService;

    public StepResponse create(Workflow workflow, StepRequest request) {
        Step step = Step.builder()
                .workflow(workflow)
                .name(request.getName())
                .stepType(request.getStepType())
                .stepOrder(request.getStepOrder() != null ? request.getStepOrder() : getNextOrder(workflow.getId()))
                .metadata(request.getMetadata())
                .build();
        step = stepRepository.save(step);
        log.info("Created step: {} in workflow {}", step.getName(), workflow.getId());
        return toResponse(step, false);
    }

    @Transactional(readOnly = true)
    public List<StepResponse> getStepsByWorkflow(String workflowId) {
        return stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId)
                .stream().map(s -> toResponse(s, true)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StepResponse getById(String stepId) {
        return toResponse(findById(stepId), true);
    }

    public StepResponse update(String stepId, StepRequest request) {
        Step step = findById(stepId);
        step.setName(request.getName());
        step.setStepType(request.getStepType());
        if (request.getStepOrder() != null) step.setStepOrder(request.getStepOrder());
        if (request.getMetadata() != null) step.setMetadata(request.getMetadata());
        step = stepRepository.save(step);
        return toResponse(step, true);
    }

    public void delete(String stepId) {
        Step step = findById(stepId);
        stepRepository.delete(step);
        log.info("Deleted step: {}", stepId);
    }

    public Step findById(String id) {
        return stepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Step", id));
    }

    private int getNextOrder(String workflowId) {
        List<Step> steps = stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId);
        return steps.isEmpty() ? 1 : steps.get(steps.size() - 1).getStepOrder() + 1;
    }

    private StepResponse toResponse(Step step, boolean includeRules) {
        StepResponse.StepResponseBuilder builder = StepResponse.builder()
                .id(step.getId())
                .workflowId(step.getWorkflow().getId())
                .name(step.getName())
                .stepType(step.getStepType())
                .stepOrder(step.getStepOrder())
                .metadata(step.getMetadata())
                .ruleCount(step.getRules() != null ? step.getRules().size() : 0)
                .createdAt(step.getCreatedAt())
                .updatedAt(step.getUpdatedAt());

        if (includeRules) {
            builder.rules(ruleService.getRulesByStep(step.getId()));
        }
        return builder.build();
    }
}
