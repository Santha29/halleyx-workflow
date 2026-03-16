package com.contract.demo.entity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "workflow_mapping")
@Data
public class WorkflowMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromRole;
    private String toRole;

    private boolean active;
}
