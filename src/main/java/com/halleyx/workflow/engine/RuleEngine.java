package com.halleyx.workflow.engine;

import com.halleyx.workflow.entity.Rule;
import com.halleyx.workflow.exception.RuleEngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.*;

@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);
    private static final String DEFAULT_KEYWORD = "DEFAULT";
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Evaluates rules in priority order and returns the next step ID.
     * Returns null if workflow should end.
     */
    public RuleEvaluationResult evaluate(List<Rule> rules, Map<String, Object> inputData) {
        if (rules == null || rules.isEmpty()) {
            log.info("No rules found - workflow ends here");
            return RuleEvaluationResult.builder()
                    .matched(false)
                    .nextStepId(null)
                    .message("No rules defined - workflow completed")
                    .build();
        }

        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparingInt(Rule::getPriority));

        Rule defaultRule = null;

        for (Rule rule : sorted) {
            if (DEFAULT_KEYWORD.equalsIgnoreCase(rule.getCondition().trim())) {
                defaultRule = rule;
                continue;
            }
            try {
                boolean result = evaluateCondition(rule.getCondition(), inputData);
                log.info("Rule [priority={}] condition='{}' → result={}", rule.getPriority(), rule.getCondition(), result);
                if (result) {
                    return RuleEvaluationResult.builder()
                            .matched(true)
                            .ruleId(rule.getId())
                            .condition(rule.getCondition())
                            .priority(rule.getPriority())
                            .nextStepId(rule.getNextStepId())
                            .message("Rule matched: " + rule.getCondition())
                            .build();
                }
            } catch (Exception e) {
                log.error("Rule evaluation error for condition '{}': {}", rule.getCondition(), e.getMessage());
                throw new RuleEngineException("Failed to evaluate condition: " + rule.getCondition(), e);
            }
        }

        // No rule matched - use DEFAULT
        if (defaultRule != null) {
            log.info("No rule matched. Using DEFAULT rule → nextStep={}", defaultRule.getNextStepId());
            return RuleEvaluationResult.builder()
                    .matched(true)
                    .ruleId(defaultRule.getId())
                    .condition("DEFAULT")
                    .priority(defaultRule.getPriority())
                    .nextStepId(defaultRule.getNextStepId())
                    .message("Default rule applied")
                    .build();
        }

        log.warn("No rule matched and no DEFAULT rule found - workflow terminates");
        return RuleEvaluationResult.builder()
                .matched(false)
                .nextStepId(null)
                .message("No matching rule found - workflow terminated")
                .build();
    }

    /**
     * Validates a condition string without executing it.
     */
    public boolean validateCondition(String condition, Set<String> fieldNames) {
        if (DEFAULT_KEYWORD.equalsIgnoreCase(condition.trim())) return true;
        try {
            String spel = toSpEL(condition, fieldNames);
            parser.parseExpression(spel);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean evaluateCondition(String condition, Map<String, Object> inputData) {
        String spel = toSpEL(condition, inputData.keySet());
        Expression expression = parser.parseExpression(spel);
        EvaluationContext context = buildContext(inputData);
        Boolean result = expression.getValue(context, Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    private String toSpEL(String condition, Set<String> fieldNames) {
        String spel = condition;

        // Replace contains(field, "value") → #field.contains('value')
        spel = spel.replaceAll(
                "contains\\((\\w+),\\s*['\"]([^'\"]+)['\"]\\)",
                "#$1.contains('$2')"
        );
        // Replace startsWith(field, "value") → #field.startsWith('value')
        spel = spel.replaceAll(
                "startsWith\\((\\w+),\\s*['\"]([^'\"]+)['\"]\\)",
                "#$1.startsWith('$2')"
        );
        // Replace endsWith(field, "value") → #field.endsWith('value')
        spel = spel.replaceAll(
                "endsWith\\((\\w+),\\s*['\"]([^'\"]+)['\"]\\)",
                "#$1.endsWith('$2')"
        );

        // Replace bare field names with #fieldName (skip already prefixed ones)
        for (String field : fieldNames) {
            spel = spel.replaceAll("(?<!#)\\b" + Pattern.quote(field) + "\\b", "#" + field);
        }

        // Replace single quotes around strings for SpEL compatibility (SpEL uses single quotes)
        return spel;
    }

    private EvaluationContext buildContext(Map<String, Object> inputData) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        inputData.forEach(context::setVariable);
        return context;
    }

    @lombok.Builder
    @lombok.Data
    public static class RuleEvaluationResult {
        private boolean matched;
        private String ruleId;
        private String condition;
        private Integer priority;
        private String nextStepId;
        private String message;
    }
}
