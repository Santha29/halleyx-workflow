package com.contract.demo.repository;

import com.contract.demo.entity.Contract;
import com.contract.demo.entity.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByStatus(ContractStatus status);
    List<Contract> findByClientUser_Id(Long clientUserId);
    List<Contract> findByAssignedRole(String assignedRole);
}
