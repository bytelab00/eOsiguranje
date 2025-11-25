package org.unibl.etf.eosiguranje.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.unibl.etf.eosiguranje.model.SecurityEvent;
import org.unibl.etf.eosiguranje.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    List<SecurityEvent> findByUserOrderByCreatedAtDesc(User user);

    List<SecurityEvent> findByActionOrderByCreatedAtDesc(String action);

    @Query("SELECT e FROM SecurityEvent e WHERE e.riskScore >= :threshold ORDER BY e.createdAt DESC")
    List<SecurityEvent> findHighRiskEvents(@Param("threshold") Integer threshold);

    @Query("SELECT e FROM SecurityEvent e WHERE e.createdAt >= :startTime ORDER BY e.createdAt DESC")
    List<SecurityEvent> findRecentEvents(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.action = 'LOGIN_FAIL' AND e.details LIKE CONCAT('%', :username, '%') AND e.createdAt >= :since")
    Long countFailedLoginAttempts(@Param("username") String username, @Param("since") LocalDateTime since);

    List<SecurityEvent> findByUserAndActionOrderByCreatedAtDesc(User user, String action);
}