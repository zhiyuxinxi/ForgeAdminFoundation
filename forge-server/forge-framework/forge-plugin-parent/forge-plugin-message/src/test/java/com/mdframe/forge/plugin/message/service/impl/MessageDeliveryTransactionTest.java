package com.mdframe.forge.plugin.message.service.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

class MessageDeliveryTransactionTest {
    @Test
    void standardResultCommitsAfterOriginalMessageCommit() {
        try (var f = new MessageTransactionFixture()) {
            f.publish(false, false);
            assertEquals(1, f.count("SELECT COUNT(*) FROM messages WHERE status=1"));
            assertEquals(1, f.count("SELECT COUNT(*) FROM records WHERE status=1 AND successful=2"));
        }
    }

    @Test
    void collaborationResultsCommitAtomicallyAndProviderRunsWithoutTransaction() {
        try (var f = new MessageTransactionFixture()) {
            f.channelResult(false);
            f.publish(true, false);
            assertEquals(1, f.count("SELECT COUNT(*) FROM messages WHERE status=3"));
            assertEquals(1, f.count("SELECT COUNT(*) FROM records WHERE status=3 AND successful=1 AND failed=1"));
            assertEquals(1, f.count("SELECT COUNT(*) FROM recipients WHERE status='SENT'"));
            assertEquals(1, f.count("SELECT COUNT(*) FROM recipients WHERE status='FAILED'"));
        }
    }

    @Test
    void latePersistenceFailureRollsBackAllResultsButKeepsCommittedMessageAndRecipients() {
        try (var f = new MessageTransactionFixture()) {
            f.channelResult(false);
            f.failFinalWrite = true;
            f.publish(true, false);
            assertEquals(1, f.count("SELECT COUNT(*) FROM messages WHERE status=0"));
            assertEquals(0, f.count("SELECT COUNT(*) FROM records"));
            assertEquals(2, f.count("SELECT COUNT(*) FROM recipients WHERE status='PENDING'"));
        }
    }

    @Test
    void originalRollbackNeverSendsOrPersistsAResult() {
        try (var f = new MessageTransactionFixture()) {
            f.publish(true, true);
            verifyNoInteractions(f.client);
            assertEquals(0, f.count("SELECT COUNT(*) FROM messages"));
            assertEquals(0, f.count("SELECT COUNT(*) FROM recipients"));
            assertEquals(0, f.count("SELECT COUNT(*) FROM records"));
        }
    }

    @Test
    void providerExceptionPersistsAFailureRecordAndStatusInANewTransaction() {
        try (var f = new MessageTransactionFixture()) {
            f.channelResult(true);
            f.publish(true, false);
            assertEquals(1, f.count("SELECT COUNT(*) FROM messages WHERE status=2"));
            assertEquals(1, f.count("SELECT COUNT(*) FROM records WHERE status=2 AND failed=2"));
            assertEquals(2, f.count("SELECT COUNT(*) FROM recipients"));
        }
    }
}
