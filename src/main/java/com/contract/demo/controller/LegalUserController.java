package com.contract.demo.controller;

import com.contract.demo.dto.CreateContractRequest;
import com.contract.demo.dto.LegalReviewRequest;
import com.contract.demo.service.LegalUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/legal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LEGAL_USER')")
public class LegalUserController {

    private final LegalUserService legalUserService;

    @PostMapping("/contracts")
    public ResponseEntity<?> createContract(@RequestBody CreateContractRequest request) {
        return ResponseEntity.ok(legalUserService.createContract(request));
    }

    @GetMapping("/contracts")
    public ResponseEntity<?> getContracts() {
        return ResponseEntity.ok(legalUserService.getLegalContracts());
    }

    @PostMapping("/review")
    public ResponseEntity<?> review(@RequestBody LegalReviewRequest request) {
        return ResponseEntity.ok(legalUserService.reviewContract(request));
    }
}
