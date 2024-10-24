package com.dws.challenge.web;

import com.dws.challenge.Request.TransferRequest;
import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  private final NotificationService notificationService;

  @Autowired
  public AccountsController(AccountsService accountsService, NotificationService notificationService) {
    this.accountsService = accountsService;
    this.notificationService = notificationService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
    this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  @PostMapping("/transfer")
  public ResponseEntity<String> transferMoney(@RequestBody TransferRequest transferRequest) {
    // Retrieve the accounts from the service (or repository)
    Account fromAccount = accountsService.getAccount(transferRequest.getFromAccountId());
    Account toAccount = accountsService.getAccount(transferRequest.getToAccountId());

    if (fromAccount == null || toAccount == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Account not found.");
    }

    // Call the transfer method
    try {
      accountsService.transfer(fromAccount, toAccount, transferRequest.getAmount());
      notificationService.notifyAboutTransfer(fromAccount, String.valueOf(transferRequest.getAmount()));
      notificationService.notifyAboutTransfer(toAccount, String.valueOf(transferRequest.getAmount()));
      return ResponseEntity.ok("Transfer successful.");
    } catch (Exception e) {
      // Catch any transfer-related exceptions
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Transfer failed: " + e.getMessage());
    }
  }
}
