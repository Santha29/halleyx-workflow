package com.halleyx.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halleyx.workflow.dto.request.WorkflowRequest;
import com.halleyx.workflow.dto.response.StepResponse;
import com.halleyx.workflow.dto.response.WorkflowResponse;
import com.halleyx.workflow.entity.Workflow;
import com.halleyx.workflow.exception.ResourceNotFoundException;
import com.halleyx.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);
    private final WorkflowRepository workflowRepository;
    private final StepService stepService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkflowResponse create(WorkflowRequest request) {
        Workflow workflow = Workflow.builder()
                .name(request.getName())
                .description(request.getDescription())
                .inputSchema(request.getInputSchema())
                .startStepId(request.getStartStepId())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .version(1)
                .build();
        workflow = workflowRepository.save(workflow);
        log.info("Created workflow: {} (id={})", workflow.getName(), workflow.getId());
        return toResponse(workflow, false);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowResponse> getAll(String name, Boolean isActive, Pageable pageable) {
        return workflowRepository.search(name, isActive, pageable)
                .map(w -> toResponse(w, false));
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getById(String id) {
        Workflow workflow = findById(id);
        return toResponse(workflow, true);
    }

    public WorkflowResponse update(String id, WorkflowRequest request) {
        Workflow workflow = findById(id);
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setInputSchema(request.getInputSchema());
        if (request.getStartStepId() != null) workflow.setStartStepId(request.getStartStepId());
        if (request.getIsActive() != null) workflow.setIsActive(request.getIsActive());
        workflow.setVersion(workflow.getVersion() + 1);  // Increment version on update
        workflow = workflowRepository.save(workflow);
        log.info("Updated workflow: {} (id={}, version={})", workflow.getName(), workflow.getId(), workflow.getVersion());
        return toResponse(workflow, true);
    }

    public void delete(String id) {
        Workflow workflow = findById(id);
        workflowRepository.delete(workflow);
        log.info("Deleted workflow: {} (id={})", workflow.getName(), id);
    }

    public Workflow findById(String id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", id));
    }

    private WorkflowResponse toResponse(Workflow workflow, boolean includeSteps) {
        WorkflowResponse.WorkflowResponseBuilder builder = WorkflowResponse.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .version(workflow.getVersion())
                .isActive(workflow.getIsActive())
                .inputSchema(workflow.getInputSchema())
                .startStepId(workflow.getStartStepId())
                .stepCount(workflow.getSteps() != null ? workflow.getSteps().size() : 0)
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt());

        if (includeSteps) {
            builder.steps(stepService.getStepsByWorkflow(workflow.getId()));
        }
        return builder.build();
    }
}
