package org.unibl.etf.eosiguranje.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unibl.etf.eosiguranje.model.UserPolicy;
import org.unibl.etf.eosiguranje.repository.UserPolicyRepository;

import java.util.List;

@Service
public class UserPolicyService {

    private final UserPolicyRepository userPolicyRepository;

    @Autowired
    public UserPolicyService(UserPolicyRepository userPolicyRepository) {
        this.userPolicyRepository = userPolicyRepository;
    }

    public UserPolicy save(UserPolicy userPolicy) {
        return userPolicyRepository.save(userPolicy);
    }

    public List<UserPolicy> findByUserId(Long userId) {
        return userPolicyRepository.findByUserId(userId);
    }
}