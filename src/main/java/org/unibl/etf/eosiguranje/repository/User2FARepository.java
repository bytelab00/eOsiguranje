
package org.unibl.etf.eosiguranje.repository;

import org.unibl.etf.eosiguranje.model.User2FA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface User2FARepository extends JpaRepository<User2FA, Long> {
    Optional<User2FA> findByUserIdAndUsedFalse(Long userId);

    // New: fetch by the specific 2FA record id, ensure it's unused and not expired
    Optional<User2FA> findByIdAndUsedFalseAndExpiresAtAfter(Long id, LocalDateTime now);
}