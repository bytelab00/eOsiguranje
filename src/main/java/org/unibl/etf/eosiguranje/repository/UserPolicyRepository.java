package org.unibl.etf.eosiguranje.repository;

import org.unibl.etf.eosiguranje.model.UserPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserPolicyRepository extends JpaRepository<UserPolicy, Long> {
    List<UserPolicy> findByUserId(Long userId);
}
