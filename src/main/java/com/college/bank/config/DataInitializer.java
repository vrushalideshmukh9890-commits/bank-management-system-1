package com.college.bank.config;

import com.college.bank.model.Account;
import com.college.bank.model.Transaction;
import com.college.bank.repository.AccountRepository;
import com.college.bank.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return args -> {
            if (accountRepository.count() == 0) {
                // Initialize default demo accounts to wow the college panel
                Account acc1 = new Account("602310842911", "Aarav Sharma", "9823456789", new BigDecimal("45000.00"), "Savings Account");
                acc1.setCreatedAt(LocalDateTime.now().minusDays(15));
                accountRepository.save(acc1);

                Account acc2 = new Account("602381923044", "Priya Patel", "9876543210", new BigDecimal("78500.50"), "Current Account");
                acc2.setCreatedAt(LocalDateTime.now().minusDays(25));
                accountRepository.save(acc2);

                Account acc3 = new Account("602355419823", "Rohan Kulkarni", "9123456780", new BigDecimal("18200.00"), "Savings Account");
                acc3.setCreatedAt(LocalDateTime.now().minusDays(8));
                accountRepository.save(acc3);

                // Initial transactions for acc1
                transactionRepository.save(new Transaction("TXN1001", acc1, "DEPOSIT", new BigDecimal("45000.00"), new BigDecimal("45000.00"), "Initial Deposit"));
                transactionRepository.save(new Transaction("TXN1002", acc1, "DEPOSIT", new BigDecimal("12000.00"), new BigDecimal("57000.00"), "Salary Credit"));
                transactionRepository.save(new Transaction("TXN1003", acc1, "WITHDRAWAL", new BigDecimal("5000.00"), new BigDecimal("52000.00"), "ATM Cash Withdrawal"));
                transactionRepository.save(new Transaction("TXN1004", acc1, "WITHDRAWAL", new BigDecimal("7000.00"), new BigDecimal("45000.00"), "Shopping Payment"));

                // Initial transactions for acc2
                transactionRepository.save(new Transaction("TXN2001", acc2, "DEPOSIT", new BigDecimal("78500.50"), new BigDecimal("78500.50"), "Initial Business Capital"));
                transactionRepository.save(new Transaction("TXN2002", acc2, "DEPOSIT", new BigDecimal("25000.00"), new BigDecimal("103500.50"), "Client Wire Transfer"));
                transactionRepository.save(new Transaction("TXN2003", acc2, "WITHDRAWAL", new BigDecimal("25000.00"), new BigDecimal("78500.50"), "Vendor Invoice Settlement"));

                // Initial transactions for acc3
                transactionRepository.save(new Transaction("TXN3001", acc3, "DEPOSIT", new BigDecimal("18200.00"), new BigDecimal("18200.00"), "Initial Savings Deposit"));
            }
        };
    }
}
