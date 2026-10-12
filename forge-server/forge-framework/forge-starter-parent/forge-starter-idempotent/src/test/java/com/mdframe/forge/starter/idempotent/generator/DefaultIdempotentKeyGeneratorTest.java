package com.mdframe.forge.starter.idempotent.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DefaultIdempotentKeyGeneratorTest {

    private final DefaultIdempotentKeyGenerator generator =
            new DefaultIdempotentKeyGenerator(new ObjectMapper());
    private final LoginUser user = new LoginUser();
    private MockedStatic<SessionHelper> identity;

    @BeforeEach
    void trustedIdentity() {
        user.setUserId(100L);
        user.setTenantId(7L);
        identity = mockStatic(SessionHelper.class);
        identity.when(SessionHelper::getLoginUser).thenReturn(user);
    }

    @AfterEach
    void closeIdentity() {
        identity.close();
    }

    @Test
    void sameArgumentsAreIsolatedByTrustedUserAndTenant() throws Exception {
        Object request = Map.of("tenantId", 99, "userId", 999, "code", "same");
        String first = generate(request);
        user.setUserId(200L);
        String secondUser = generate(request);
        user.setUserId(100L);
        user.setTenantId(8L);
        String secondTenant = generate(request);
        assertThat(first).isNotEqualTo(secondUser).isNotEqualTo(secondTenant);
        assertThat(first).matches("idem:[a-f0-9]{64}");
    }

    @Test
    void missingTrustedIdentityFailsClosedForDefaultKeys() {
        identity.when(SessionHelper::getLoginUser).thenReturn(null);
        assertDenied();
        identity.when(SessionHelper::getLoginUser).thenReturn(user);
        user.setTenantId(null);
        assertDenied();
        user.setTenantId(7L);
        user.setUserId(null);
        assertDenied();
    }

    @Test
    void explicitSpelRetainsExactCallerDefinedScopeWithoutImplicitIdentity() throws Exception {
        identity.when(SessionHelper::getLoginUser).thenReturn(null);
        assertThat(generator.generate(point(Object.class, null), "custom:", "'shared-business-key'"))
                .isEqualTo("custom:shared-business-key");
        identity.verifyNoInteractions();
    }

    @Test
    void overloadedMethodsWithSameSerializedArgumentsHaveDifferentKeys() throws Exception {
        assertThat(generator.generate(point(Object.class, null), "idem:", ""))
                .isNotEqualTo(generator.generate(point(String.class, null), "idem:", ""));
    }

    private void assertDenied() {
        assertThatThrownBy(() -> generate("same")).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getCode()).isEqualTo(403));
    }

    @Test
    void shouldUsePojoContentInsteadOfObjectToString() throws Exception {
        String first = generate(new Request("A", 1));
        String sameContent = generate(new Request("A", 1));
        String different = generate(new Request("B", 1));

        assertThat(first).isEqualTo(sameContent);
        assertThat(first).isNotEqualTo(different);
    }

    @Test
    void shouldGenerateSameKeyForMapsWithDifferentInsertionOrder() throws Exception {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("tenantId", 1L);
        first.put("code", "A");
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("code", "A");
        second.put("tenantId", 1L);

        assertThat(generate(first)).isEqualTo(generate(second));
    }

    private String generate(Object argument) throws Exception {
        return generator.generate(point(Object.class, argument), "idem:", "");
    }

    private ProceedingJoinPoint point(Class<?> argumentType, Object argument) throws Exception {
        Method method = SampleService.class.getDeclaredMethod("submit", argumentType);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{argument});
        return joinPoint;
    }

    static class SampleService {
        void submit(Object request) {
        }

        void submit(String request) {
        }
    }

    record Request(String code, int version) {
    }
}
