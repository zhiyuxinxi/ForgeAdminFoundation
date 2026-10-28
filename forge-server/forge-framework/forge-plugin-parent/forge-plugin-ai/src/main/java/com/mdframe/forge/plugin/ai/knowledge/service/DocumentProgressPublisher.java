package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.service.dto.DocumentProcessEvent;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Owns in-process progress subscriptions; persisted processing state remains the source of truth. */
final class DocumentProgressPublisher {
    private final ConcurrentHashMap<Long, Set<Sinks.Many<DocumentProcessEvent>>> subscriptions =
            new ConcurrentHashMap<>();

    Flux<ServerSentEvent<DocumentProcessEvent>> subscribe(Long documentId) {
        return Flux.defer(() -> {
            Sinks.Many<DocumentProcessEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
            subscriptions.compute(documentId, (id, current) -> {
                Set<Sinks.Many<DocumentProcessEvent>> listeners = current == null
                        ? ConcurrentHashMap.newKeySet() : current;
                listeners.add(sink);
                return listeners;
            });
            return sink.asFlux()
                    .map(event -> ServerSentEvent.<DocumentProcessEvent>builder()
                            .id(String.valueOf(System.nanoTime()))
                            .event("progress")
                            .data(event)
                            .build())
                    .doFinally(signal -> subscriptions.computeIfPresent(documentId, (id, listeners) -> {
                        listeners.remove(sink);
                        return listeners.isEmpty() ? null : listeners;
                    }));
        });
    }

    void emit(DocumentProcessEvent event) {
        Set<Sinks.Many<DocumentProcessEvent>> listeners = subscriptions.get(event.getDocumentId());
        if (listeners != null) {
            listeners.forEach(sink -> sink.tryEmitNext(event));
        }
    }
}
