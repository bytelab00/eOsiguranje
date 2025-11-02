package org.unibl.etf.eosiguranje.service;

import org.unibl.etf.eosiguranje.model.Policy;
import org.unibl.etf.eosiguranje.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PolicyService {
    private final PolicyRepository policyRepository;

    public List<Policy> findAll() {
        return policyRepository.findAll();
    }

    public Optional<Policy> findById(Long id) {
        return policyRepository.findById(id);
    }

    public Policy save(Policy policy) {
        return policyRepository.save(policy);
    }

    public void deleteById(Long id) {
        policyRepository.deleteById(id);
    }
}
