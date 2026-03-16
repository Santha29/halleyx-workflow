package com.contract.demo.dto;

import com.contract.demo.entity.ContractStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractResponse {

    private Long id;
    private String contractName;
    private BigDecimal contractAmount;
    private ContractStatus status;
    private String assignedRole;
}
