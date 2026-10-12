package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** In-memory Redis command substitute; no network or real Redis proof is implied. */
final class ApprovalRedis {
    final Map<String, String> values = new HashMap<>();
    final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    final InterruptStore store = new InterruptStore(redis);

    ApprovalRedis() {
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        doAnswer(call -> {
            synchronized (values) {
                values.put(call.getArgument(0), call.getArgument(1));
            }
            return null;
        })
                .when(operations).set(anyString(), anyString(), eq(10L), eq(TimeUnit.MINUTES));
        when(operations.get(anyString())).thenAnswer(call -> { synchronized (values) { return values.get(call.getArgument(0)); } });
        when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenAnswer(call -> {
            List<String> keys = call.getArgument(1);
            String expected = call.getArgument(2);
            synchronized (values) {
                if (expected.equals(values.get(keys.get(0)))) {
                    values.remove(keys.get(0));
                    return 1L;
                }
                return 0L;
            }
        });
    }
}
