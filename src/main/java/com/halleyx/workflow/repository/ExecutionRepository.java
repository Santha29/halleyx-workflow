package com.halleyx.workflow.repository;

import com.halleyx.workflow.entity.Execution;
import com.halleyx.workflow.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, String> {
    Page<Execution> findByWorkflowId(String workflowId, Pageable pageable);
    Page<Execution> findByStatus(ExecutionStatus status, Pageable pageable);
    List<Execution> findByWorkflowIdAndStatus(String workflowId, ExecutionStatus status);
    long countByWorkflowId(String workflowId);
    long countByStatus(ExecutionStatus status);
}
