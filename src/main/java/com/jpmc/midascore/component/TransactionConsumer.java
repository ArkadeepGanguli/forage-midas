package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);

    private final DatabaseConduit databaseConduit;

    public TransactionConsumer(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-group")
    public void consumeTransaction(Transaction transaction) {
        logger.info("========================================");
        logger.info("TRANSACTION RECEIVED!");
        logger.info("Amount: {}", transaction.getAmount());
        logger.info("Sender: {}", transaction.getSenderId());
        logger.info("Recipient: {}", transaction.getRecipientId());

        boolean applied = databaseConduit.applyTransactionIfValid(transaction.getSenderId(), transaction.getRecipientId(), transaction.getAmount());
        if (!applied) {
            logger.info("Transaction invalid or could not be applied: {} -> {} : {}", transaction.getSenderId(), transaction.getRecipientId(), transaction.getAmount());
        }

        logger.info("========================================");
    }
}