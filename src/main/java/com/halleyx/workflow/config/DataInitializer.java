package com.halleyx.workflow.config;

import com.halleyx.workflow.entity.*;
import com.halleyx.workflow.enums.StepType;
import com.halleyx.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final WorkflowRepository workflowRepository;
    private final StepRepository stepRepository;
    private final RuleRepository ruleRepository;

    @Override
    public void run(String... args) {
        if (workflowRepository.count() > 0) return;
        log.info("Seeding sample workflows...");

        // === Workflow 1: Expense Approval ===
        Workflow expenseWf = workflowRepository.save(Workflow.builder()
                .name("Expense Approval")
                .description("Multi-step expense approval workflow with finance review and CEO sign-off")
                .version(1).isActive(true)
                .inputSchema("{\"required\":[\"amount\",\"country\",\"priority\"],\"fields\":{\"amount\":\"number\",\"country\":\"string\",\"priority\":\"string\"}}")
                .build());

        Step managerApproval = stepRepository.save(Step.builder().workflow(expenseWf)
                .name("Manager Approval").stepType(StepType.APPROVAL).stepOrder(1)
                .metadata("{\"assignee_email\":\"manager@company.com\",\"instructions\":\"Review expense claim\"}")
                .build());

        Step financeNotification = stepRepository.save(Step.builder().workflow(expenseWf)
                .name("Finance Notification").stepType(StepType.NOTIFICATION).stepOrder(2)
                .metadata("{\"notification_channel\":\"email\",\"template\":\"finance-review-template\"}")
                .build());

        Step ceoApproval = stepRepository.save(Step.builder().workflow(expenseWf)
                .name("CEO Approval").stepType(StepType.APPROVAL).stepOrder(3)
                .metadata("{\"assignee_email\":\"ceo@company.com\",\"instructions\":\"Final approval for high-value expenses\"}")
                .build());

        Step taskCompletion = stepRepository.save(Step.builder().workflow(expenseWf)
                .name("Task Completion").stepType(StepType.TASK).stepOrder(4)
                .metadata("{\"instructions\":\"Mark expense as approved and notify requester\"}")
                .build());

        Step taskRejection = stepRepository.save(Step.builder().workflow(expenseWf)
                .name("Task Rejection").stepType(StepType.TASK).stepOrder(5)
                .metadata("{\"instructions\":\"Mark expense as rejected and notify requester\"}")
                .build());

        expenseWf.setStartStepId(managerApproval.getId());
        workflowRepository.save(expenseWf);

        // Manager Approval rules
        ruleRepository.save(Rule.builder().step(managerApproval).priority(1)
                .condition("amount > 100 && country == 'US' && priority == 'High'")
                .nextStepId(financeNotification.getId())
                .description("High value US expense → Finance notification").build());
        ruleRepository.save(Rule.builder().step(managerApproval).priority(2)
                .condition("amount <= 100")
                .nextStepId(taskCompletion.getId())
                .description("Low value expense → Direct completion").build());
        ruleRepository.save(Rule.builder().step(managerApproval).priority(3)
                .condition("priority == 'Low' && country != 'US'")
                .nextStepId(taskRejection.getId())
                .description("Low priority non-US → Reject").build());
        ruleRepository.save(Rule.builder().step(managerApproval).priority(4)
                .condition("DEFAULT").nextStepId(taskRejection.getId())
                .description("Default → Reject").build());

        // Finance Notification rules
        ruleRepository.save(Rule.builder().step(financeNotification).priority(1)
                .condition("amount > 10000")
                .nextStepId(ceoApproval.getId())
                .description("Very high value → CEO approval").build());
        ruleRepository.save(Rule.builder().step(financeNotification).priority(2)
                .condition("DEFAULT").nextStepId(taskCompletion.getId())
                .description("Default → Complete").build());

        // CEO Approval rules
        ruleRepository.save(Rule.builder().step(ceoApproval).priority(1)
                .condition("DEFAULT").nextStepId(taskCompletion.getId())
                .description("CEO approved → Complete").build());

        // Task Completion — end of workflow
        ruleRepository.save(Rule.builder().step(taskCompletion).priority(1)
                .condition("DEFAULT").nextStepId(null)
                .description("Workflow ends here").build());
        ruleRepository.save(Rule.builder().step(taskRejection).priority(1)
                .condition("DEFAULT").nextStepId(null)
                .description("Workflow ends here").build());

        // === Workflow 2: Employee Onboarding ===
        Workflow onboardingWf = workflowRepository.save(Workflow.builder()
                .name("Employee Onboarding")
                .description("Automated employee onboarding with account setup and welcome notification")
                .version(1).isActive(true)
                .inputSchema("{\"required\":[\"department\",\"role\"],\"fields\":{\"department\":\"string\",\"role\":\"string\"}}")
                .build());

        Step accountSetup = stepRepository.save(Step.builder().workflow(onboardingWf)
                .name("Account Setup").stepType(StepType.TASK).stepOrder(1)
                .metadata("{\"instructions\":\"Create user accounts and assign system access\"}")
                .build());

        Step hrApproval = stepRepository.save(Step.builder().workflow(onboardingWf)
                .name("HR Approval").stepType(StepType.APPROVAL).stepOrder(2)
                .metadata("{\"assignee_email\":\"hr@company.com\",\"instructions\":\"Approve onboarding for new employee\"}")
                .build());

        Step welcomeNotification = stepRepository.save(Step.builder().workflow(onboardingWf)
                .name("Welcome Notification").stepType(StepType.NOTIFICATION).stepOrder(3)
                .metadata("{\"notification_channel\":\"slack\",\"template\":\"welcome-employee-template\"}")
                .build());

        onboardingWf.setStartStepId(accountSetup.getId());
        workflowRepository.save(onboardingWf);

        ruleRepository.save(Rule.builder().step(accountSetup).priority(1)
                .condition("department == 'Engineering'")
                .nextStepId(hrApproval.getId())
                .description("Engineering → HR approval required").build());
        ruleRepository.save(Rule.builder().step(accountSetup).priority(2)
                .condition("DEFAULT").nextStepId(welcomeNotification.getId())
                .description("Others → Direct welcome notification").build());

        ruleRepository.save(Rule.builder().step(hrApproval).priority(1)
                .condition("DEFAULT").nextStepId(welcomeNotification.getId())
                .description("After HR approval → Welcome").build());

        ruleRepository.save(Rule.builder().step(welcomeNotification).priority(1)
                .condition("DEFAULT").nextStepId(null)
                .description("Onboarding complete").build());

        log.info("Sample data seeded: 2 workflows, {} steps", stepRepository.count());
    }
}
