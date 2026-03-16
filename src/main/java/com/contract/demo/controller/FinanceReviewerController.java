package com.contract.demo.controller;

import com.contract.demo.dto.FinanceReviewRequest;
import com.contract.demo.entity.Contract;
import com.contract.demo.service.FinanceReviewerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceReviewerController {

    private final FinanceReviewerService service;

    @PostMapping("/review")
    @PreAuthorize("hasRole('FINANCE_REVIEWER')")
    public ResponseEntity<?> review(@RequestBody FinanceReviewRequest request,
                                    Authentication authentication) {

        Contract result = service.reviewContract(request, authentication.getName());
        return ResponseEntity.ok(result);
    }
}
