package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.system.mapper.TenantOrganizationDirectoryMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.identity.TenantOrganization;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SystemTenantOrganizationDirectoryTest {
    private final TenantOrganizationDirectoryMapper mapper = mock(TenantOrganizationDirectoryMapper.class);
    private final SystemTenantOrganizationDirectory directory = new SystemTenantOrganizationDirectory(mapper);

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        DynamicDataSourceContextHolder.clear();
        DataScopeContextHolder.clearSkip();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void rejectsMissingOrInconsistentIdentityBeforeAnyLookup() {
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            assertCode(401, () -> directory.findActiveOrganizations(List.of()));
            assertCode(401, () -> directory.findActiveChildren(0L, null, 20));
            LoginUser caller = new LoginUser();
            caller.setUserId(9L);
            session.when(SessionHelper::getLoginUser).thenReturn(caller);
            assertCode(401, () -> directory.findActiveOrganizations(List.of(10L)));
            caller.setTenantId(2L);
            TenantContextHolder.setTenantId(3L);
            assertCode(403, () -> directory.findActiveOrganizations(List.of(10L)));
            assertCode(403, () -> directory.findActiveChildren(0L, null, 20));
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void emptyDoesNotMeanAllAndInvalidOrUnboundedQueriesAreRejected() {
        try (MockedStatic<SessionHelper> ignored = SystemTenantMemberDirectoryTest.login()) {
            assertThat(directory.findActiveOrganizations(List.of())).isEmpty();
            assertCode(400, () -> directory.findActiveOrganizations(null));
            assertCode(400, () -> directory.findActiveOrganizations(Arrays.asList(10L, null)));
            assertCode(400, () -> directory.findActiveOrganizations(List.of(0L)));
            assertCode(400, () -> directory.findActiveOrganizations(List.of(-1L)));
            assertCode(400, () -> directory.findActiveOrganizations(Collections.nCopies(501, 10L)));
            assertCode(400, () -> directory.findActiveChildren(null, null, 20));
            assertCode(400, () -> directory.findActiveChildren(-1L, null, 20));
            assertCode(400, () -> directory.findActiveChildren(0L, 0L, 20));
            assertCode(400, () -> directory.findActiveChildren(0L, -1L, 20));
            assertCode(400, () -> directory.findActiveChildren(0L, null, 0));
            assertCode(400, () -> directory.findActiveChildren(0L, null, 201));
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void readsMasterAndReturnsAnImmutableResultWithoutChangingCallerScope() {
        TenantOrganization org = new TenantOrganization(10L, 0L, "同名组织");
        when(mapper.selectActiveOrganizations(2L, List.of(10L))).thenAnswer(call -> {
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("master");
            assertThat(DataScopeContextHolder.isSkip()).isTrue();
            assertThat(TenantContextHolder.isIgnore()).isFalse();
            return List.of(org);
        });
        DynamicDataSourceContextHolder.push("business-db");
        TenantContextHolder.setTenantId(2L);
        try (MockedStatic<SessionHelper> ignored = SystemTenantMemberDirectoryTest.login()) {
            List<TenantOrganization> result = directory.findActiveOrganizations(List.of(10L, 10L));
            assertThat(result).containsExactly(org);
            assertThatThrownBy(() -> result.add(org)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
            assertThat(DataScopeContextHolder.isSkip()).isFalse();
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(2L);
        }
    }

    @Test
    void lookupFailureRestoresNormalAndPreviouslySkippedCallerContexts() {
        when(mapper.selectActiveChildren(2L, 0L, 10L, 200))
                .thenThrow(new IllegalStateException("database unavailable"));
        DynamicDataSourceContextHolder.push("business-db");
        try (MockedStatic<SessionHelper> ignored = SystemTenantMemberDirectoryTest.login()) {
            for (boolean alreadySkipping : new boolean[]{false, true}) {
                if (alreadySkipping) {
                    DataScopeContextHolder.skipDataScope();
                }
                assertThatThrownBy(() -> directory.findActiveChildren(0L, 10L, 200))
                        .isInstanceOf(IllegalStateException.class);
                assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
                assertThat(DataScopeContextHolder.isSkip()).isEqualTo(alreadySkipping);
            }
        }
    }

    @Test
    void invalidInputUsesTheRequestLocaleWithoutChangingErrorCode() {
        try (MockedStatic<SessionHelper> ignored = SystemTenantMemberDirectoryTest.login()) {
            LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
            assertThatThrownBy(() -> directory.findActiveChildren(null, null, 20))
                    .isInstanceOfSatisfying(BusinessException.class, error -> {
                        assertThat(error.getCode()).isEqualTo(400);
                        assertThat(error.getMessage()).isEqualTo("组织目录查询参数超出允许范围");
                    });
            LocaleContextHolder.setLocale(Locale.US);
            assertThatThrownBy(() -> directory.findActiveChildren(null, null, 20))
                    .isInstanceOfSatisfying(BusinessException.class, error -> {
                        assertThat(error.getCode()).isEqualTo(400);
                        assertThat(error.getMessage()).isEqualTo("Organization lookup parameters are outside the allowed bounds");
                    });
        }
    }

    private void assertCode(int code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getCode()).isEqualTo(code));
    }
}
