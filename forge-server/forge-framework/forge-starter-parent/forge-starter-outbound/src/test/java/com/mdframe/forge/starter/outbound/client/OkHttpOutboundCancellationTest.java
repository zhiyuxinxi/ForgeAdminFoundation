package com.mdframe.forge.starter.outbound.client;

import com.mdframe.forge.starter.outbound.config.OutboundProperties;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.ValidatedOutboundTarget;
import com.mdframe.forge.starter.outbound.security.IpAddressClassifier;
import com.mdframe.forge.starter.outbound.security.OutboundPolicyService;
import com.mdframe.forge.starter.outbound.security.OutboundSecurityException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OkHttpOutboundCancellationTest {
    @Test
    void cancellationClosesBlockedSocketAndUnregistersOnlyThisCall() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            OutboundPolicyService policy = mock(OutboundPolicyService.class);
            var uri = server.url("/blocked").uri();
            when(policy.validate(any())).thenReturn(new ValidatedOutboundTarget(OutboundScenes.AI_HTTP_TOOL,
                    uri, uri.getHost(), uri.getPort(), List.of(InetAddress.getByName("127.0.0.1")),
                    IpAddressClassifier.AddressType.PUBLIC));
            OutboundProperties properties = new OutboundProperties();
            properties.setReadTimeout(Duration.ofSeconds(20));
            properties.setCallTimeout(Duration.ofSeconds(20));
            var client = new OkHttpSecureOutboundClient(policy, properties);
            var request = OutboundRequest.builder().scene(OutboundScenes.AI_HTTP_TOOL).url(uri.toString()).method("GET").build();
            AtomicReference<Runnable> cancellation = new AtomicReference<>();
            var executor = Executors.newSingleThreadExecutor();
            try {
                var pending = executor.submit(() -> client.execute(request, cancellation::set));
                assertNotNull(server.takeRequest(2, TimeUnit.SECONDS));
                assertNotNull(cancellation.get());
                cancellation.get().run();
                ExecutionException failure = assertThrows(ExecutionException.class, () -> pending.get(2, TimeUnit.SECONDS));
                assertInstanceOf(OutboundSecurityException.class, failure.getCause());
                assertNull(cancellation.get());
                server.enqueue(new MockResponse().setBody("next"));
                assertEquals("next", client.execute(request, cancellation::set).bodyAsUtf8());
                assertNull(cancellation.get());
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
