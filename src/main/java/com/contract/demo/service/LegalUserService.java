package com.contract.demo.service;

import com.contract.demo.dto.CreateContractRequest;
import com.contract.demo.dto.LegalReviewRequest;
import com.contract.demo.entity.Contract;
import com.contract.demo.entity.ContractStatus;
import com.contract.demo.repository.ContractRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LegalUserService {

    private final ContractRepository contractRepository;

    // View contracts assigned to Legal
    public List<Contract> getLegalContracts() {
        return contractRepository.findByAssignedRole("LEGAL_USER");
    }

    public Contract createContract(CreateContractRequest request) {

        Contract contract = new Contract();

        contract.setContractName(request.getContractName());
        contract.setClientId(request.getClientId());
        contract.setEffectiveDate(request.getEffectiveDate());
        contract.setContractAmount(request.getContractAmount());

        contract.setStatus(ContractStatus.LEGAL_REVIEW);
        contract.setAssignedRole("LEGAL_USER");
        contract.setActive(true);

        return contractRepository.save(contract);
    }


    // Review and forward contract
    public Contract reviewContract(LegalReviewRequest request) {

        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (!"LEGAL_USER".equals(contract.getAssignedRole())) {
            throw new RuntimeException("Contract not assigned to LEGAL_USER");
        }

        if (request.isApproved()) {
            contract.setStatus(ContractStatus.FINANCE_REVIEW);
            contract.setAssignedRole("FINANCE_REVIEWER");
        } else {
            contract.setStatus(ContractStatus.REJECTED);
        }

        return contractRepository.save(contract);
    }
}
