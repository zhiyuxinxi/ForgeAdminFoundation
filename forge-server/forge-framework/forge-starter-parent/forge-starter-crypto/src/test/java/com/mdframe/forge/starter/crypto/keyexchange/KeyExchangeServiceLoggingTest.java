package com.mdframe.forge.starter.crypto.keyexchange;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mdframe.forge.starter.cache.service.ICacheService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KeyExchangeServiceLoggingTest {
    @Test void successfulExchangeAndStorageNeverLogCredentialsEvenAtDebugLevel() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.mdframe.forge.starter.crypto.keyexchange");
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        Level previous = logger.getLevel();
        events.start();
        logger.addAppender(events);
        logger.setLevel(Level.DEBUG);
        try {
            RsaKeyPairHolder rsa = mock(RsaKeyPairHolder.class);
            ICacheService cache = mock(ICacheService.class);
            SessionKeyStore store = new SessionKeyStore(cache, 60);
            KeyExchangeService service = new KeyExchangeService(rsa, store);
            when(rsa.decryptByPrivateKey("encrypted-test-key")).thenReturn("clear-test-key");

            assertThat(service.exchangeKey("test-bearer-token", "encrypted-test-key")).isTrue();
            store.removeKey("test-bearer-token");
            verify(cache).set("crypto:session:test-bearer-token", "clear-test-key", 60L,
                    java.util.concurrent.TimeUnit.SECONDS);
            assertThat(events.list).isNotEmpty().allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain("test-bearer-token", "clear-test-key", "encrypted-test-key");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(events);
            logger.setLevel(previous);
            events.stop();
        }
    }

    @Test void failureDoesNotLogSensitiveUpstreamExceptionMessages() {
        Logger logger = (Logger) LoggerFactory.getLogger(KeyExchangeService.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        Level previous = logger.getLevel();
        events.start();
        logger.addAppender(events);
        logger.setLevel(Level.ERROR);
        try {
            RsaKeyPairHolder rsa = mock(RsaKeyPairHolder.class);
            SessionKeyStore store = mock(SessionKeyStore.class);
            when(rsa.decryptByPrivateKey("encrypted-test-key"))
                    .thenThrow(new IllegalArgumentException("test-bearer-token clear-test-key encrypted-test-key"));
            assertThat(new KeyExchangeService(rsa, store)
                    .exchangeKey("test-bearer-token", "encrypted-test-key")).isFalse();
            verifyNoInteractions(store);
            assertThat(events.list).isNotEmpty().allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("IllegalArgumentException")
                        .doesNotContain("test-bearer-token", "clear-test-key", "encrypted-test-key");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(events);
            logger.setLevel(previous);
            events.stop();
        }
    }
}
