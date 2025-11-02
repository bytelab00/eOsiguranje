package org.unibl.etf.eosiguranje.service;

import org.unibl.etf.eosiguranje.model.Transaction;
import org.unibl.etf.eosiguranje.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public List<Transaction> findByUserId(Long userId) {
        return transactionRepository.findByUserId(userId);
    }
}
