package org.unibl.etf.eosiguranje.repository;

import org.unibl.etf.eosiguranje.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
}
