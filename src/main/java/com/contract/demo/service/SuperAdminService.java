package com.contract.demo.service;

import com.contract.demo.dto.AssignRoleRequest;
import com.contract.demo.dto.CreateUserRequest;
import com.contract.demo.dto.WorkflowMappingRequest;
import com.contract.demo.entity.*;
import com.contract.demo.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SuperAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WorkflowRepository workflowRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    // ================= USER MANAGEMENT =================

    public User createUser(CreateUserRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        audit("CREATE_USER", "Created user: " + savedUser.getUsername());

        return savedUser;
    }

    // ================= ROLE ASSIGNMENT =================

    public void assignRoles(AssignRoleRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        user.setRoles(roles);
        userRepository.save(user);

        audit("ASSIGN_ROLE", "User: " + user.getUsername() + " Roles: " + roles);
    }

    // ================= WORKFLOW MAPPING =================

    public WorkflowMapping mapWorkflow(WorkflowMappingRequest request) {

        WorkflowMapping mapping = new WorkflowMapping();
        mapping.setFromRole(request.getFromRole());
        mapping.setToRole(request.getToRole());
        mapping.setActive(true);

        WorkflowMapping saved = workflowRepository.save(mapping);

        audit("MAP_WORKFLOW", request.getFromRole() + " -> " + request.getToRole());

        return saved;
    }

    // ================= VIEW ALL USERS =================

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ================= AUDIT LOGS =================

    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAll();
    }

    // ================= AUDIT UTILITY =================

    private void audit(String action, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setDetails(details);
        log.setActor("superadmin");
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }


    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
