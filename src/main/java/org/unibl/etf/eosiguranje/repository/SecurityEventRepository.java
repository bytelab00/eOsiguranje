package org.unibl.etf.eosiguranje.repository;

import org.unibl.etf.eosiguranje.model.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    List<SecurityEvent> findByUserId(Long userId);


}
