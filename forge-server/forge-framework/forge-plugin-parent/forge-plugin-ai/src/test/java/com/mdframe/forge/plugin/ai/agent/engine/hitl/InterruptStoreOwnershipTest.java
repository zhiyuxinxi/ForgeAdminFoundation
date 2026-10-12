package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InterruptStoreOwnershipTest {
    @Test
    void redisLookupAndRemovalMustUseCallerTenantAndOwner() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        Map<String, String> storage = new HashMap<>();
        when(redis.opsForValue()).thenReturn(values);
        doAnswer(call -> storage.put(call.getArgument(0), call.getArgument(1))).when(values)
                .set(anyString(), anyString(), eq(10L), eq(TimeUnit.MINUTES));
        when(values.get(anyString())).thenAnswer(call -> storage.get(call.getArgument(0)));
        when(redis.delete(anyString())).thenAnswer(call -> storage.remove(call.getArgument(0)) != null);
        InterruptStore store = new InterruptStore(redis);
        ReactContext context = new ReactContext();
        context.setSessionId("session");
        context.setUserId(10L);
        context.setTenantId(20L);
        String id = store.save(context);
        assertTrue(storage.containsKey("agent:interrupt:20:10:" + id));
        try (var identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getUserId).thenReturn(11L);
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            assertNull(store.get(id));
            store.remove(id);
            assertEquals(1, storage.size());
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            identity.when(SessionHelper::getTenantId).thenReturn(21L);
            assertNull(store.get(id));
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            assertEquals("session", store.get(id).getContext().getSessionId());
            store.remove(id);
            assertTrue(storage.isEmpty());
        }
    }
}
