package com.contract.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateContractRequest {

    private String contractName;
    private Long clientId;
    private LocalDate effectiveDate;
    private BigDecimal contractAmount;
}
