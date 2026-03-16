package com.halleyx.workflow.entity;

import com.halleyx.workflow.enums.StepStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "step_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Execution execution;

    @Column(name = "step_id")
    private String stepId;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "step_type")
    private String stepType;

    @Enumerated(EnumType.STRING)
    private StepStatus status;

    @Column(name = "rule_evaluated", columnDefinition = "TEXT")
    private String ruleEvaluated;

    @Column(name = "next_step_id")
    private String nextStepId;

    @Column(name = "next_step_name")
    private String nextStepName;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "approver_email")
    private String approverEmail;

    @Column(name = "duration_ms")
    private Long durationMs;

    @CreationTimestamp
    @Column(name = "executed_at", updatable = false)
    private LocalDateTime executedAt;
}
