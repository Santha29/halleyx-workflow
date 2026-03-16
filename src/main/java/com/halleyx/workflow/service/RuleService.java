package com.halleyx.workflow.service;

import com.halleyx.workflow.dto.request.RuleRequest;
import com.halleyx.workflow.dto.response.RuleResponse;
import com.halleyx.workflow.entity.Rule;
import com.halleyx.workflow.entity.Step;
import com.halleyx.workflow.exception.ResourceNotFoundException;
import com.halleyx.workflow.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RuleService {

    private final RuleRepository ruleRepository;

    public RuleResponse create(Step step, RuleRequest request) {
        Rule rule = Rule.builder()
                .step(step)
                .condition(request.getCondition())
                .nextStepId(request.getNextStepId())
                .priority(request.getPriority())
                .description(request.getDescription())
                .build();
        rule = ruleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> getRulesByStep(String stepId) {
        return ruleRepository.findByStepIdOrderByPriorityAsc(stepId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RuleResponse getById(String ruleId) {
        return toResponse(findById(ruleId));
    }

    public RuleResponse update(String ruleId, RuleRequest request) {
        Rule rule = findById(ruleId);
        rule.setCondition(request.getCondition());
        rule.setNextStepId(request.getNextStepId());
        rule.setPriority(request.getPriority());
        if (request.getDescription() != null) rule.setDescription(request.getDescription());
        rule = ruleRepository.save(rule);
        return toResponse(rule);
    }

    public void delete(String ruleId) {
        Rule rule = findById(ruleId);
        ruleRepository.delete(rule);
    }

    public Rule findById(String id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", id));
    }

    private RuleResponse toResponse(Rule rule) {
        return RuleResponse.builder()
                .id(rule.getId())
                .stepId(rule.getStep().getId())
                .condition(rule.getCondition())
                .nextStepId(rule.getNextStepId())
                .priority(rule.getPriority())
                .description(rule.getDescription())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
