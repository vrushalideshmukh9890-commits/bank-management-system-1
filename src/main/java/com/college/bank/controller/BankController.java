package com.college.bank.controller;

import com.college.bank.model.Account;
import com.college.bank.model.Transaction;
import com.college.bank.service.BankService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    /**
     * Dashboard page
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        Map<String, Object> stats = bankService.getDashboardStats();
        List<Transaction> recentTransactions = bankService.getRecentTransactions();
        List<Account> accounts = bankService.getAllAccounts();

        model.addAttribute("stats", stats);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("accounts", accounts);
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }

    /**
     * Create Account form
     */
    @GetMapping("/create-account")
    public String showCreateAccountForm(Model model) {
        model.addAttribute("activePage", "create-account");
        return "create-account";
    }

    /**
     * Process Account Creation
     */
    @PostMapping("/create-account")
    public String processCreateAccount(
            @RequestParam("holderName") String holderName,
            @RequestParam("mobileNumber") String mobileNumber,
            @RequestParam("openingDeposit") BigDecimal openingDeposit,
            @RequestParam(value = "accountType", defaultValue = "Savings Account") String accountType,
            RedirectAttributes redirectAttributes) {

        try {
            Account account = bankService.createAccount(holderName, mobileNumber, openingDeposit, accountType);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully!");
            redirectAttributes.addFlashAttribute("createdAccount", account);
            return "redirect:/account-success";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("holderName", holderName);
            redirectAttributes.addFlashAttribute("mobileNumber", mobileNumber);
            redirectAttributes.addFlashAttribute("openingDeposit", openingDeposit);
            return "redirect:/create-account";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to create account: " + e.getMessage());
            return "redirect:/create-account";
        }
    }

    /**
     * Success page after account creation
     */
    @GetMapping("/account-success")
    public String accountSuccess(Model model) {
        model.addAttribute("activePage", "create-account");
        return "account-success";
    }

    /**
     * Account Details & operations page
     */
    @GetMapping("/account-details")
    public String accountDetails(
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            Model model) {

        model.addAttribute("activePage", "account-details");
        model.addAttribute("allAccounts", bankService.getAllAccounts());
        model.addAttribute("dailyLimit", BankService.DAILY_WITHDRAWAL_LIMIT);

        if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            accountNumber = accountNumber.trim();
            Optional<Account> accountOpt = bankService.getAccount(accountNumber);
            if (accountOpt.isPresent()) {
                Account account = accountOpt.get();
                List<Transaction> transactions = bankService.getAccountTransactions(accountNumber);
                BigDecimal todayWithdrawn = bankService.getTodayWithdrawn(accountNumber);
                BigDecimal remainingLimit = BankService.DAILY_WITHDRAWAL_LIMIT.subtract(todayWithdrawn);
                if (remainingLimit.compareTo(BigDecimal.ZERO) < 0) {
                    remainingLimit = BigDecimal.ZERO;
                }

                model.addAttribute("account", account);
                model.addAttribute("transactions", transactions);
                model.addAttribute("todayWithdrawn", todayWithdrawn);
                model.addAttribute("remainingLimit", remainingLimit);
            } else {
                model.addAttribute("errorMessage", "No account found with number: " + accountNumber);
            }
        }
        return "account-details";
    }

    /**
     * Deposit action from Account Details
     */
    @PostMapping("/deposit")
    public String handleDeposit(
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "description", required = false) String description,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.deposit(accountNumber, amount, description);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Successfully deposited ₹%,.2f into account %s.", amount, accountNumber));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/account-details?accountNumber=" + accountNumber;
    }

    /**
     * Withdraw action from Account Details
     */
    @PostMapping("/withdraw")
    public String handleWithdraw(
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "description", required = false) String description,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.withdraw(accountNumber, amount, description);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Successfully withdrew ₹%,.2f from account %s.", amount, accountNumber));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/account-details?accountNumber=" + accountNumber;
    }

    /**
     * Digital Passbook
     */
    @GetMapping("/passbook")
    public String passbook(
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {

        model.addAttribute("activePage", "passbook");
        model.addAttribute("allAccounts", bankService.getAllAccounts());

        LocalDate defaultFrom = (fromDate != null) ? fromDate : LocalDate.now().minusMonths(1);
        LocalDate defaultTo = (toDate != null) ? toDate : LocalDate.now();
        model.addAttribute("fromDate", defaultFrom);
        model.addAttribute("toDate", defaultTo);

        if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            accountNumber = accountNumber.trim();
            Optional<Account> accountOpt = bankService.getAccount(accountNumber);
            if (accountOpt.isPresent()) {
                model.addAttribute("account", accountOpt.get());
                try {
                    List<Transaction> statement = bankService.getPassbookStatement(accountNumber, defaultFrom, defaultTo);
                    model.addAttribute("transactions", statement);
                    model.addAttribute("searched", true);
                } catch (IllegalArgumentException e) {
                    model.addAttribute("errorMessage", e.getMessage());
                }
            } else {
                model.addAttribute("errorMessage", "Account " + accountNumber + " not found.");
            }
        }
        return "passbook";
    }

    /**
     * Analytics & Graphics page (featuring Bresenham Line Drawing Algorithm)
     */
    @GetMapping("/analytics")
    public String analytics(Model model) {
        Map<String, Object> stats = bankService.getDashboardStats();
        List<Account> accounts = bankService.getAllAccounts();
        List<Transaction> transactions = bankService.getRecentTransactions();

        model.addAttribute("stats", stats);
        model.addAttribute("accounts", accounts);
        model.addAttribute("transactions", transactions);
        model.addAttribute("activePage", "analytics");
        return "analytics";
    }

    /**
     * REST endpoint for real-time chart data
     */
    @GetMapping("/api/chart-data")
    @ResponseBody
    public Map<String, Object> getChartData() {
        return bankService.getDashboardStats();
    }
}
