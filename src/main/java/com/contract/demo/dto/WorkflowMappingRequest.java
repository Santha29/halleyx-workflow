package com.contract.demo.dto;

import lombok.Data;

@Data
public class WorkflowMappingRequest {

    private String fromRole;
    private String toRole;
}
