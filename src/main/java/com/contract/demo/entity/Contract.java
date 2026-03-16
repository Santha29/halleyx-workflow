package com.contract.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
@Data
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_name", nullable = false)
    private String contractName;

    @Column(name = "legal_user_id")
    private Long legalUserId;

    @Column(name = "finance_user_id")
    private Long financeUserId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "contract_amount", precision = 15, scale = 2)
    private BigDecimal contractAmount;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    private Boolean active;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "assigned_role")
    private String assignedRole;

    @Column(length = 5000)
    private String content;

    private String title;

    @ManyToOne
    @JoinColumn(name = "client_user_id")
    private User clientUser;

    @Column(length = 500)
    private String clientRemarks;

}
