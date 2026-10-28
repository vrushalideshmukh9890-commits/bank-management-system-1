package com.college.bank.service;

import com.college.bank.model.Account;
import com.college.bank.model.Transaction;
import com.college.bank.repository.AccountRepository;
import com.college.bank.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BankService {

    public static final BigDecimal DAILY_WITHDRAWAL_LIMIT = new BigDecimal("20000.00");
    public static final BigDecimal MIN_OPENING_BALANCE = new BigDecimal("500.00");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BankService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Generate unique 12-digit Indian account number (starts with 6023...)
     */
    public synchronized String generateAccountNumber() {
        Random random = new Random();
        String accNo;
        do {
            long number = 602300000000L + (long) (random.nextDouble() * 99999999L);
            accNo = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(accNo));
        return accNo;
    }

    private String generateTxnId() {
        return "TXN" + System.currentTimeMillis() + (new Random().nextInt(900) + 100);
    }

    /**
     * Create Account with initial deposit
     */
    @Transactional
    public Account createAccount(String holderName, String mobileNumber, BigDecimal openingDeposit, String accountType) {
        if (openingDeposit == null || openingDeposit.compareTo(MIN_OPENING_BALANCE) < 0) {
            throw new IllegalArgumentException("Minimum initial deposit of ₹" + MIN_OPENING_BALANCE + " is required.");
        }
        if (holderName == null || holderName.trim().length() < 3) {
            throw new IllegalArgumentException("Account holder name must be at least 3 characters long.");
        }
        if (mobileNumber == null || !mobileNumber.matches("^[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("Valid 10-digit Indian mobile number is required.");
        }

        String accountNumber = generateAccountNumber();
        Account account = new Account(accountNumber, holderName.trim(), mobileNumber.trim(), openingDeposit, accountType);
        account = accountRepository.save(account);

        // Record initial deposit transaction
        Transaction initialTxn = new Transaction(
                generateTxnId(),
                account,
                "DEPOSIT",
                openingDeposit,
                openingDeposit,
                "Initial Account Opening Deposit"
        );
        transactionRepository.save(initialTxn);

        return account;
    }

    /**
     * Deposit money into account
     */
    @Transactional
    public Transaction deposit(String accountNumber, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountNumber));

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction txn = new Transaction(
                generateTxnId(),
                account,
                "DEPOSIT",
                amount,
                newBalance,
                (description != null && !description.isBlank()) ? description.trim() : "Cash Deposit"
        );
        return transactionRepository.save(txn);
    }

    /**
     * Withdraw money with balance and daily limit check
     */
    @Transactional
    public Transaction withdraw(String accountNumber, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountNumber));

        // 1. Available balance check
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Insufficient balance! Available balance is ₹%,.2f, but requested amount is ₹%,.2f",
                    account.getBalance(), amount));
        }

        // 2. Daily withdrawal limit check (₹20,000 per day)
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal todayWithdrawn = transactionRepository.getDailyWithdrawalTotal(accountNumber, dayStart, dayEnd);
        BigDecimal projectedToday = todayWithdrawn.add(amount);

        if (projectedToday.compareTo(DAILY_WITHDRAWAL_LIMIT) > 0) {
            BigDecimal remainingLimit = DAILY_WITHDRAWAL_LIMIT.subtract(todayWithdrawn);
            if (remainingLimit.compareTo(BigDecimal.ZERO) < 0) {
                remainingLimit = BigDecimal.ZERO;
            }
            throw new IllegalArgumentException(String.format(
                    "Daily withdrawal limit exceeded! Daily limit: ₹%,.2f. Already withdrawn today: ₹%,.2f. Remaining allowance: ₹%,.2f. Requested: ₹%,.2f",
                    DAILY_WITHDRAWAL_LIMIT, todayWithdrawn, remainingLimit, amount));
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction txn = new Transaction(
                generateTxnId(),
                account,
                "WITHDRAWAL",
                amount,
                newBalance,
                (description != null && !description.isBlank()) ? description.trim() : "ATM / Cash Withdrawal"
        );
        return transactionRepository.save(txn);
    }

    public Optional<Account> getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public List<Transaction> getRecentTransactions() {
        return transactionRepository.findTop10ByOrderByTransactionDateDesc();
    }

    public List<Transaction> getAccountTransactions(String accountNumber) {
        return transactionRepository.findByAccountNumberOrderByTransactionDateDesc(accountNumber);
    }

    public List<Transaction> getPassbookStatement(String accountNumber, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) {
            fromDate = LocalDate.now().minusMonths(1);
        }
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after To date.");
        }
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);
        return transactionRepository.findPassbookStatement(accountNumber, start, end);
    }

    public BigDecimal getTodayWithdrawn(String accountNumber) {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX);
        return transactionRepository.getDailyWithdrawalTotal(accountNumber, dayStart, dayEnd);
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalAccounts = accountRepository.count();
        long totalTransactions = transactionRepository.count();
        BigDecimal totalDeposits = transactionRepository.getTotalDeposits();
        BigDecimal totalWithdrawals = transactionRepository.getTotalWithdrawals();
        BigDecimal totalBankBalance = accountRepository.getTotalBankBalance();

        stats.put("totalAccounts", totalAccounts);
        stats.put("totalTransactions", totalTransactions);
        stats.put("totalDeposits", totalDeposits != null ? totalDeposits : BigDecimal.ZERO);
        stats.put("totalWithdrawals", totalWithdrawals != null ? totalWithdrawals : BigDecimal.ZERO);
        stats.put("totalBankBalance", totalBankBalance != null ? totalBankBalance : BigDecimal.ZERO);

        return stats;
    }
}
