package com.halleyx.workflow.repository;

import com.halleyx.workflow.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RuleRepository extends JpaRepository<Rule, String> {
    List<Rule> findByStepIdOrderByPriorityAsc(String stepId);
    void deleteByStepId(String stepId);
}
