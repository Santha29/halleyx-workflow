package com.contract.demo.dto;

import lombok.Data;

@Data
public class FinanceReviewRequest {
    private Long contractId;
    private boolean approved;
    private String remarks;
}
