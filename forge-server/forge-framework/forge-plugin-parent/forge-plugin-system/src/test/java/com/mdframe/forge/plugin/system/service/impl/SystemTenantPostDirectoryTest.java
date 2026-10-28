package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.system.mapper.TenantPostDirectoryMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.identity.TenantPost;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SystemTenantPostDirectoryTest {
    private final TenantPostDirectoryMapper mapper = mock(TenantPostDirectoryMapper.class);
    private final SystemTenantPostDirectory directory = new SystemTenantPostDirectory(mapper);

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        DynamicDataSourceContextHolder.clear();
        DataScopeContextHolder.clearSkip();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void rejectsMissingIdentityAndMismatchedTenantEvenForEmptyInput() {
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            assertCode(401, () -> directory.findActivePosts(List.of(), null));
            LoginUser caller = new LoginUser();
            caller.setUserId(9L);
            session.when(SessionHelper::getLoginUser).thenReturn(caller);
            assertCode(401, () -> directory.findActivePosts(List.of(101L), null));
            caller.setTenantId(2L);
            TenantContextHolder.setTenantId(3L);
            assertCode(403, () -> directory.findActivePosts(List.of(101L), null));
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void rejectsUnboundedOrMalformedQueriesAndEmptyDoesNotMeanAll() {
        try (MockedStatic<SessionHelper> ignored = login()) {
            assertThat(directory.findActivePosts(List.of(), null)).isEmpty();
            assertCode(400, () -> directory.findActivePosts(null, null));
            assertCode(400, () -> directory.findActivePosts(List.of(101L), 0L));
            assertCode(400, () -> directory.findActivePostsByOrg(-1L, null, null, 20));
            assertCode(400, () -> directory.findActivePosts(Arrays.asList(101L, null), null));
            assertCode(400, () -> directory.findActivePosts(List.of(0L), null));
            assertCode(400, () -> directory.findActivePosts(Collections.nCopies(501, 101L), null));
            assertCode(400, () -> directory.findActivePostsByOrg(null, null, null, 20));
            assertCode(400, () -> directory.findActivePostsByOrg(10L, null, null, 201));
            assertCode(400, () -> directory.findActivePostsByOrg(10L, null, null, 0));
            assertCode(400, () -> directory.findActivePostsByOrg(10L, null, -1L, 20));
            assertCode(400, () -> directory.findActivePostsByOrg(10L, "x".repeat(101), null, 20));
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void readsMasterWithExplicitIdentityScopeAndRestoresCallerContext() {
        TenantPost member = new TenantPost(101L, 10L, "technician", "同名");
        when(mapper.selectActivePosts(2L, List.of(101L), 10L)).thenAnswer(call -> {
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("master");
            assertThat(DataScopeContextHolder.isSkip()).isTrue();
            assertThat(TenantContextHolder.isIgnore()).isFalse();
            return List.of(member);
        });
        DynamicDataSourceContextHolder.push("business-db");
        TenantContextHolder.setTenantId(2L);
        try (MockedStatic<SessionHelper> ignored = login()) {
            List<TenantPost> result = directory.findActivePosts(List.of(101L, 101L), 10L);
            assertThat(result).containsExactly(member);
            assertThatThrownBy(() -> result.add(member)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
            assertThat(DataScopeContextHolder.isSkip()).isFalse();
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(2L);
        }
    }

    @Test
    void queryFailureRestoresBothPreviouslySkippedAndNormalCallerContexts() {
        when(mapper.selectActivePostsByOrg(2L, 10L, "同名", 101L, 20))
                .thenThrow(new IllegalStateException("database unavailable"));
        DynamicDataSourceContextHolder.push("business-db");
        try (MockedStatic<SessionHelper> ignored = login()) {
            for (boolean alreadySkipping : new boolean[]{false, true}) {
                if (alreadySkipping) {
                    DataScopeContextHolder.skipDataScope();
                }
                assertThatThrownBy(() -> directory.findActivePostsByOrg(10L, " 同名 ", 101L, 20))
                        .isInstanceOf(IllegalStateException.class);
                assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
                assertThat(DataScopeContextHolder.isSkip()).isEqualTo(alreadySkipping);
            }
        }
    }

    @Test
    void exactBatchAndPageLimitsRemainUsableWithoutChangingTheTrustedTenant() {
        when(mapper.selectActivePosts(2L, List.of(101L), null)).thenReturn(List.of());
        when(mapper.selectActivePostsByOrg(2L, 10L, null, null, 200)).thenReturn(List.of());
        try (MockedStatic<SessionHelper> ignored = login()) {
            assertThat(directory.findActivePosts(Collections.nCopies(500, 101L), null)).isEmpty();
            assertThat(directory.findActivePostsByOrg(10L, "  ", null, 200)).isEmpty();
        }
    }

    @Test
    void invalidParametersUseTheRequestLanguageWithStableErrorCodes() {
        try (MockedStatic<SessionHelper> ignored = login()) {
            LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
            assertThatThrownBy(() -> directory.findActivePostsByOrg(null, null, null, 20))
                    .isInstanceOfSatisfying(BusinessException.class, error -> {
                        assertThat(error.getCode()).isEqualTo(400);
                        assertThat(error.getMessage()).isEqualTo("岗位目录查询参数超出允许范围");
                    });
            LocaleContextHolder.setLocale(Locale.US);
            assertThatThrownBy(() -> directory.findActivePostsByOrg(null, null, null, 20))
                    .isInstanceOfSatisfying(BusinessException.class, error -> {
                        assertThat(error.getCode()).isEqualTo(400);
                        assertThat(error.getMessage()).isEqualTo("Post lookup parameters are outside the allowed bounds");
                    });
        }
    }

    @Test
    void tenantWideLookupIsExplicitBoundedAndUsesOnlyTheAuthenticatedTenant() {
        try (MockedStatic<SessionHelper> ignored = login()) {
            assertCode(400, () -> directory.findActiveTenantWidePosts(null, null, 0));
            assertCode(400, () -> directory.findActiveTenantWidePosts(null, null, 201));
            assertCode(400, () -> directory.findActiveTenantWidePosts(null, 0L, 20));
            assertCode(400, () -> directory.findActiveTenantWidePosts("x".repeat(101), null, 20));
            TenantPost global = new TenantPost(103L, null, "GLOBAL", "租户级岗位");
            when(mapper.selectActiveTenantWidePosts(2L, "GLOBAL", null, 20)).thenReturn(List.of(global));
            assertThat(directory.findActiveTenantWidePosts(" GLOBAL ", null, 20)).containsExactly(global);
        }
    }

    static MockedStatic<SessionHelper> login() {
        LoginUser caller = new LoginUser();
        caller.setUserId(9L);
        caller.setTenantId(2L);
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(caller);
        return session;
    }

    private void assertCode(int code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getCode()).isEqualTo(code));
    }
}
