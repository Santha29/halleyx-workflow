package com.halleyx.workflow.entity;

import com.halleyx.workflow.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "workflow_name")
    private String workflowName;

    @Column(name = "workflow_version")
    private Integer workflowVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "current_step_id")
    private String currentStepId;

    @Column(name = "current_step_name")
    private String currentStepName;

    @Column(name = "retries")
    @Builder.Default
    private Integer retries = 0;

    @Column(name = "triggered_by")
    private String triggeredBy;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("executedAt ASC")
    @Builder.Default
    private List<StepLog> logs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
