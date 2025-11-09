package org.unibl.etf.eosiguranje.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.unibl.etf.eosiguranje.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

    @Repository
    public interface PolicyRepository extends JpaRepository<Policy, Long> {
        @Query("SELECT p.price FROM Policy p WHERE p.id = :policyId")
        double getExpectedPrice(@Param("policyId") long policyId);
    }
