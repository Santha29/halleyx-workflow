package com.contract.demo.controller;

import com.contract.demo.dto.AssignRoleRequest;
import com.contract.demo.dto.CreateUserRequest;
import com.contract.demo.dto.WorkflowMappingRequest;
import com.contract.demo.service.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService service;

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(service.createUser(request));
    }

    @PutMapping("/assign-role")
    public ResponseEntity<?> assignRole(@RequestBody AssignRoleRequest request) {
        service.assignRoles(request);
        return ResponseEntity.ok("Roles assigned successfully");
    }

    @PostMapping("/workflow")
    public ResponseEntity<?> mapWorkflow(@RequestBody WorkflowMappingRequest request) {
        return ResponseEntity.ok(service.mapWorkflow(request));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @GetMapping("/audit")
    public ResponseEntity<?> auditLogs() {
        return ResponseEntity.ok(service.getAuditLogs());
    }
}
