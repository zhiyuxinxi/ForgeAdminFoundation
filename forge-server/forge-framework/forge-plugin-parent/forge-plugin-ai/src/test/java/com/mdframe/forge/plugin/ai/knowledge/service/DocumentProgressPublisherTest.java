package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.service.dto.DocumentProcessEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentProgressPublisherTest {
    @Test
    void independentSubscriptionsBothReceiveTheDocumentProgress() {
        DocumentProgressPublisher publisher = new DocumentProgressPublisher();
        List<Integer> first = new ArrayList<>();
        List<Integer> second = new ArrayList<>();
        Disposable firstConnection = publisher.subscribe(1L).subscribe(event -> first.add(event.data().getProgress()));
        Disposable secondConnection = publisher.subscribe(1L).subscribe(event -> second.add(event.data().getProgress()));
        try {
            publisher.emit(progress(1L, 30));
            assertEquals(List.of(30), first);
            assertEquals(List.of(30), second);
        } finally {
            firstConnection.dispose();
            secondConnection.dispose();
        }
    }

    @Test
    void cancelingAnOlderConnectionDoesNotUnregisterTheLiveOrReconnectedSubscriber() {
        DocumentProgressPublisher publisher = new DocumentProgressPublisher();
        Flux<ServerSentEvent<DocumentProcessEvent>> stream = publisher.subscribe(1L);
        List<Integer> first = new ArrayList<>();
        List<Integer> second = new ArrayList<>();
        List<Integer> replacement = new ArrayList<>();
        Disposable firstConnection = stream.subscribe(event -> first.add(event.data().getProgress()));
        Disposable secondConnection = stream.subscribe(event -> second.add(event.data().getProgress()));
        firstConnection.dispose();
        publisher.emit(progress(1L, 50));
        Disposable replacementConnection = stream.subscribe(event -> replacement.add(event.data().getProgress()));
        try {
            secondConnection.dispose();
            publisher.emit(progress(1L, 100));
            assertEquals(List.of(), first);
            assertEquals(List.of(50), second);
            assertEquals(List.of(100), replacement);
        } finally {
            secondConnection.dispose();
            replacementConnection.dispose();
        }
    }

    @Test
    void progressIsRestrictedToItsDocument() {
        DocumentProgressPublisher publisher = new DocumentProgressPublisher();
        List<Integer> first = new ArrayList<>();
        List<Integer> other = new ArrayList<>();
        Disposable firstConnection = publisher.subscribe(1L).subscribe(event -> first.add(event.data().getProgress()));
        Disposable otherConnection = publisher.subscribe(2L).subscribe(event -> other.add(event.data().getProgress()));
        try {
            publisher.emit(progress(1L, 30));
            assertEquals(List.of(30), first);
            assertEquals(List.of(), other);
        } finally {
            firstConnection.dispose();
            otherConnection.dispose();
        }
    }

    private static DocumentProcessEvent progress(Long documentId, int value) {
        return DocumentProcessEvent.of(documentId, "example", "parsing", value, "progress", "processing");
    }
}
