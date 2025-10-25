package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Component
public class DatabaseConduit {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConduit.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;

    public DatabaseConduit(UserRepository userRepository, TransactionRepository transactionRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
    }

    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);
    }

    public UserRecord findUserById(long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public boolean applyTransactionIfValid(long senderId, long recipientId, float amount) {
        UserRecord sender = userRepository.findById(senderId);
        UserRecord recipient = userRepository.findById(recipientId);

        if (sender == null || recipient == null) {
            logger.info("applyTransactionIfValid: sender or recipient not found ({} -> {})", senderId, recipientId);
            return false;
        }

        if (sender.getBalance() < amount) {
            logger.info("applyTransactionIfValid: insufficient balance for sender {} (balance={} amount={})", senderId, sender.getBalance(), amount);
            return false;
        }

        // adjust balances
        float oldSenderBalance = sender.getBalance();
        float oldRecipientBalance = recipient.getBalance();

        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount);

        // Call incentive API
        float incentive = 0f;
        try {
            // the incentive API expects a Transaction JSON; we will post to http://localhost:8080/incentive
            com.jpmc.midascore.foundation.Transaction tx = new com.jpmc.midascore.foundation.Transaction(senderId, recipientId, amount);
            ResponseEntity<com.jpmc.midascore.component.IncentiveResponse> response = restTemplate.postForEntity("http://localhost:8080/incentive", tx, com.jpmc.midascore.component.IncentiveResponse.class);
            if (response != null && response.getBody() != null) {
                incentive = response.getBody().getAmount();
            }
        } catch (Exception e) {
            logger.info("Failed to call incentive API: {}", e.toString());
            incentive = 0f;
        }

        // apply incentive to recipient balance (not deducted from sender)
        recipient.setBalance(recipient.getBalance() + incentive);

        // persist users and transaction record
        userRepository.save(sender);
        userRepository.save(recipient);

        TransactionRecord record = new TransactionRecord(sender, recipient, amount, incentive);
        transactionRepository.save(record);

        logger.info("Transaction applied: {} -> {} amount={} incentive={} | senderBalance: {}->{} recipientBalance: {}->{}",
                senderId, recipientId, amount, incentive, oldSenderBalance, sender.getBalance(), oldRecipientBalance, recipient.getBalance());

        return true;
    }

}
