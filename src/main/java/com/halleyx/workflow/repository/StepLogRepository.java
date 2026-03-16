package com.halleyx.workflow.repository;

import com.halleyx.workflow.entity.StepLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StepLogRepository extends JpaRepository<StepLog, String> {
    List<StepLog> findByExecutionIdOrderByExecutedAtAsc(String executionId);
}
