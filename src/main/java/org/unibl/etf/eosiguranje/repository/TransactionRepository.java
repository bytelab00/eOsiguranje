package org.unibl.etf.eosiguranje.repository;

import org.unibl.etf.eosiguranje.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByStripePaymentIntentId(String stripePaymentIntentId);

    //List<Transaction> findByUserId(Long userId);
}
