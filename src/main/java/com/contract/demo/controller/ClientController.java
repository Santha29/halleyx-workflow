package com.contract.demo.controller;

import com.contract.demo.dto.ClientDecisionRequest;
import com.contract.demo.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService service;

    @GetMapping("/contracts")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> myContracts(Authentication auth) {
        return ResponseEntity.ok(service.getAssignedContracts(auth.getName()));
    }

    @PostMapping("/review")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> review(@RequestBody ClientDecisionRequest request,
                                    Authentication auth) {

        return ResponseEntity.ok(service.review(request, auth.getName()));
    }
}
