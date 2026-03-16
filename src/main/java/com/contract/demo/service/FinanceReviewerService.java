package com.contract.demo.service;

import com.contract.demo.dto.FinanceReviewRequest;
import com.contract.demo.entity.AuditLog;
import com.contract.demo.entity.Contract;
import com.contract.demo.entity.ContractStatus;
import com.contract.demo.repository.AuditLogRepository;
import com.contract.demo.repository.ContractRepository;
import com.contract.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceReviewerService {

    private final ContractRepository contractRepository;
    private final AuditLogRepository auditRepo;

    public Contract reviewContract(FinanceReviewRequest request, String reviewer) {

        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (contract.getStatus() != ContractStatus.LEGAL_REVIEW) {
            throw new RuntimeException("Only LEGAL_REVIEW contracts can be reviewed by Finance");
        }

        if (request.isApproved()) {
            contract.setStatus(ContractStatus.FINANCE_APPROVED);
        } else {
            contract.setStatus(ContractStatus.FINANCE_REJECTED);
        }

        contract.setFinanceUserId(getCurrentUserId());

        audit("FINANCE_REVIEW", reviewer,
                "Contract " + contract.getId() + " -> " + contract.getStatus() +
                        " Remarks: " + request.getRemarks());

        return contractRepository.save(contract);
    }

    private void audit(String action, String actor, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setActor(actor);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        auditRepo.save(log);
    }

        UserRepository userRepository;
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();
    }

}
