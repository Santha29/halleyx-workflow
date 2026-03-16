package com.contract.demo.entity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "role_permissions")
@Data
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Role role;

    @ManyToOne
    private Permission permission;
}
