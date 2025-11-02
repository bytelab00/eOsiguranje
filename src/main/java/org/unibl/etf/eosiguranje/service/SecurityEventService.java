package org.unibl.etf.eosiguranje.service;

import org.unibl.etf.eosiguranje.model.SecurityEvent;
import org.unibl.etf.eosiguranje.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityEventService {
    private final SecurityEventRepository securityEventRepository;

    public SecurityEvent save(SecurityEvent event) {
        return securityEventRepository.save(event);
    }

    public List<SecurityEvent> findByUserId(Long userId) {
        return securityEventRepository.findByUserId(userId);
    }
}
