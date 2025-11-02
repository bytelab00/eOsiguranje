package org.unibl.etf.eosiguranje.service;

import org.unibl.etf.eosiguranje.model.UserPolicy;
import org.unibl.etf.eosiguranje.repository.UserPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPolicyService {
    private final UserPolicyRepository userPolicyRepository;

    public UserPolicy save(UserPolicy userPolicy) {
        return userPolicyRepository.save(userPolicy);
    }

    public List<UserPolicy> findByUserId(Long userId) {
        return userPolicyRepository.findByUserId(userId);
    }
}
