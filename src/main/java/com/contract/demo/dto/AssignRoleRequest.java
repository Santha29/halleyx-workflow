package com.contract.demo.dto;

import lombok.Data;

import java.util.Set;

@Data
public class AssignRoleRequest {

    private Long userId;
    private Set<String> roles;
}
