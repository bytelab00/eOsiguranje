package org.unibl.etf.eosiguranje.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.unibl.etf.eosiguranje.model.Transaction;
import org.unibl.etf.eosiguranje.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public Transaction saveTransaction(Long userId, String username, Long policyId, BigDecimal amount, String paymentIntentId) {
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .username(username)
                .policyId(policyId)
                .amount(amount)
                .stripePaymentIntentId(paymentIntentId)
                .provider("stripe")
                .providerId(1L)
                .status("pending") // Set initial status
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }

    public Optional<Transaction> findByPaymentIntentId(String paymentIntentId) {
        return transactionRepository.findByStripePaymentIntentId(paymentIntentId);
    }

    public Transaction updateTransactionStatus(Transaction transaction, String status) {
        transaction.setStatus(status);
        return transactionRepository.save(transaction);
    }
}
