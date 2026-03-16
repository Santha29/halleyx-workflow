package com.halleyx.workflow.repository;

import com.halleyx.workflow.entity.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, String> {
    Page<Workflow> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Workflow> findByIsActive(Boolean isActive, Pageable pageable);
    @Query("SELECT w FROM Workflow w WHERE (:name IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%',:name,'%'))) AND (:isActive IS NULL OR w.isActive = :isActive)")
    Page<Workflow> search(@Param("name") String name, @Param("isActive") Boolean isActive, Pageable pageable);
    List<Workflow> findByIsActiveTrue();
}
