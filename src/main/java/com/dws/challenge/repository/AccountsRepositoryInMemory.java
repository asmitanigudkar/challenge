package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
        if (previousAccount != null) {
            throw new DuplicateAccountIdException(
                    "Account id " + account.getAccountId() + " already exists!");
        }
    }

    @Override
    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    @Override
    public void clearAccounts() {
        accounts.clear();
    }

    // Synchronized method to deposit money
    public static void deposit(double amount, Account toAccount) {
        synchronized (toAccount) {
            if (amount > 0) {
                double balance = toAccount.getBalance().doubleValue();
                balance += amount;
                System.out.println(amount + " deposited. Current balance: " + balance);
            } else {
                System.out.println("Invalid deposit amount.");
            }
        }
    }

    // Synchronized method to withdraw money
    public static void withdraw(double amount, Account fromAccount) {
        // Synchronize on the account object
        synchronized (fromAccount) {
            double balance = fromAccount.getBalance().doubleValue(); // Get the balance of the account
            if (amount > 0 && amount <= balance) {
                balance -= amount;
                fromAccount.setBalance(BigDecimal.valueOf(balance)); // Set the new balance after withdrawal
                System.out.println(amount + " withdrawn. Current balance: " + balance);
            } else {
                System.out.println("Invalid or insufficient funds for withdrawal.");
            }
        }
    }

    public void transfer(Account fromAccount, Account toAccount, double amount) {
        Account firstLock = fromAccount;
        Account secondLock = toAccount;

        if (System.identityHashCode(fromAccount) > System.identityHashCode(toAccount)) {
            firstLock = toAccount;
            secondLock = fromAccount;
        }

        // Lock both accounts
        synchronized (firstLock) {
            synchronized (secondLock) {
                if (amount > 0 && fromAccount.getBalance().doubleValue() >= amount) {
                    withdraw(amount, fromAccount);
                    deposit(amount, toAccount);
                    System.out.println("Successfully transferred " + amount + " from "
                            + fromAccount.getAccountId() + " to "
                            + toAccount.getAccountId());
                } else {
                    System.out.println("Transfer failed due to insufficient funds.");
                }
            }
        }
    }

}
