package org.unibl.etf.eosiguranje.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unibl.etf.eosiguranje.model.Policy;
import org.unibl.etf.eosiguranje.model.User;
import org.unibl.etf.eosiguranje.model.UserPolicy;
import org.unibl.etf.eosiguranje.repository.UserPolicyRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPolicyService {
    private final UserPolicyRepository userPolicyRepository;

    public UserPolicy createUserPolicy(User user, Policy policy, String pdfPath) {
        UserPolicy userPolicy = UserPolicy.builder()
                .user(user)
                .policy(policy)
                .purchaseDate(LocalDateTime.now())
                .pdfPath(pdfPath)
                .build();

        return userPolicyRepository.save(userPolicy);
    }
}