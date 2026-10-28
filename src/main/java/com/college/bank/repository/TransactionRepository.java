package com.college.bank.repository;

import com.college.bank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountNumberOrderByTransactionDateDesc(String accountNumber);

    List<Transaction> findTop10ByOrderByTransactionDateDesc();

    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accNo " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate ASC")
    List<Transaction> findPassbookStatement(
            @Param("accNo") String accountNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = 'DEPOSIT' AND t.status = 'SUCCESS'")
    BigDecimal getTotalDeposits();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = 'WITHDRAWAL' AND t.status = 'SUCCESS'")
    BigDecimal getTotalWithdrawals();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.accountNumber = :accNo " +
           "AND t.type = 'WITHDRAWAL' " +
           "AND t.status = 'SUCCESS' " +
           "AND t.transactionDate BETWEEN :dayStart AND :dayEnd")
    BigDecimal getDailyWithdrawalTotal(
            @Param("accNo") String accountNumber,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
}
