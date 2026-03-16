package com.contract.demo.service;

import com.contract.demo.dto.ClientDecisionRequest;
import com.contract.demo.entity.Contract;
import com.contract.demo.entity.ContractStatus;
import com.contract.demo.entity.User;
import com.contract.demo.repository.ContractRepository;
import com.contract.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;

    public List<Contract> getAssignedContracts(String username) {
        User client = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        return contractRepository.findByClientUser_Id(client.getId());
    }

    public Contract review(ClientDecisionRequest request, String username) {

        User client = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Authorization check
        if (!contract.getClientUser().getId().equals(client.getId())) {
            throw new RuntimeException("Unauthorized: Contract not assigned to you");
        }

        // Workflow validation
        if (contract.getStatus() != ContractStatus.FINANCE_APPROVED) {
            throw new RuntimeException("Contract not ready for client approval");
        }

        contract.setClientRemarks(request.getRemarks());

        if (request.isApproved()) {
            contract.setStatus(ContractStatus.CLIENT_APPROVED);
            contract.setStatus(ContractStatus.ACTIVE);
        } else {
            contract.setStatus(ContractStatus.CLIENT_REJECTED);
        }

        return contractRepository.save(contract);
    }
}
