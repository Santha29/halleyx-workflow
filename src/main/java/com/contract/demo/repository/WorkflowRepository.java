package com.contract.demo.repository;

import com.contract.demo.entity.WorkflowMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<WorkflowMapping, Long> {}
