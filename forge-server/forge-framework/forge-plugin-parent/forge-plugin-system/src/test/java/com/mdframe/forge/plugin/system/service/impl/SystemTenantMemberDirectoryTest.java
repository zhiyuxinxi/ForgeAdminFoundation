package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.system.mapper.TenantMemberDirectoryMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SystemTenantMemberDirectoryTest {
    private final TenantMemberDirectoryMapper mapper = mock(TenantMemberDirectoryMapper.class);
    private final SystemTenantMemberDirectory directory = new SystemTenantMemberDirectory(mapper);

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        DynamicDataSourceContextHolder.clear();
        DataScopeContextHolder.clearSkip();
    }

    @Test
    void rejectsMissingIdentityAndMismatchedTenantEvenForEmptyInput() {
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            assertCode(401, () -> directory.findActiveMembers(List.of(), null));
            LoginUser caller = new LoginUser();
            caller.setUserId(9L);
            session.when(SessionHelper::getLoginUser).thenReturn(caller);
            assertCode(401, () -> directory.findActiveMembers(List.of(101L), null));
            caller.setTenantId(2L);
            TenantContextHolder.setTenantId(3L);
            assertCode(403, () -> directory.findActiveMembers(List.of(101L), null));
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void rejectsUnboundedOrMalformedQueriesAndEmptyDoesNotMeanAll() {
        try (MockedStatic<SessionHelper> ignored = login()) {
            assertThat(directory.findActiveMembers(List.of(), null)).isEmpty();
            assertCode(400, () -> directory.findActiveMembers(null, null));
            assertCode(400, () -> directory.findActiveMembers(Arrays.asList(101L, null), null));
            assertCode(400, () -> directory.findActiveMembers(List.of(0L), null));
            assertCode(400, () -> directory.findActiveMembers(Collections.nCopies(501, 101L), null));
            assertCode(400, () -> directory.findActiveMembersByOrg(null, null, null, 20));
            assertCode(400, () -> directory.findActiveMembersByOrg(10L, null, null, 201));
            assertCode(400, () -> directory.findActiveMembersByOrg(10L, null, null, 0));
            assertCode(400, () -> directory.findActiveMembersByOrg(10L, null, -1L, 20));
            assertCode(400, () -> directory.findActiveMembersByOrg(10L, "x".repeat(101), null, 20));
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void readsMasterWithExplicitIdentityScopeAndRestoresCallerContext() {
        TenantMember member = new TenantMember(101L, "alice", "同名");
        when(mapper.selectActiveMembers(2L, List.of(101L), 10L)).thenAnswer(call -> {
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("master");
            assertThat(DataScopeContextHolder.isSkip()).isTrue();
            assertThat(TenantContextHolder.isIgnore()).isFalse();
            return List.of(member);
        });
        DynamicDataSourceContextHolder.push("business-db");
        TenantContextHolder.setTenantId(2L);
        try (MockedStatic<SessionHelper> ignored = login()) {
            List<TenantMember> result = directory.findActiveMembers(List.of(101L, 101L), 10L);
            assertThat(result).containsExactly(member);
            assertThatThrownBy(() -> result.add(member)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
            assertThat(DataScopeContextHolder.isSkip()).isFalse();
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(2L);
        }
    }

    @Test
    void queryFailureRestoresBothPreviouslySkippedAndNormalCallerContexts() {
        when(mapper.selectActiveMembersByOrg(2L, 10L, "同名", 101L, 20))
                .thenThrow(new IllegalStateException("database unavailable"));
        DynamicDataSourceContextHolder.push("business-db");
        try (MockedStatic<SessionHelper> ignored = login()) {
            for (boolean alreadySkipping : new boolean[]{false, true}) {
                if (alreadySkipping) {
                    DataScopeContextHolder.skipDataScope();
                }
                assertThatThrownBy(() -> directory.findActiveMembersByOrg(10L, " 同名 ", 101L, 20))
                        .isInstanceOf(IllegalStateException.class);
                assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
                assertThat(DataScopeContextHolder.isSkip()).isEqualTo(alreadySkipping);
            }
        }
    }

    @Test
    void postLookupRequiresAuthenticatedTenantAndBoundedStableIds() {
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            assertCode(401, () -> directory.findActiveMembersByPost(30L, null, 20));
            LoginUser caller = new LoginUser();
            caller.setUserId(9L);
            caller.setTenantId(2L);
            session.when(SessionHelper::getLoginUser).thenReturn(caller);
            TenantContextHolder.setTenantId(3L);
            assertCode(403, () -> directory.findActiveMembersByPost(30L, null, 20));
            TenantContextHolder.clear();
            assertCode(400, () -> directory.findActiveMembersByPost(null, null, 20));
            assertCode(400, () -> directory.findActiveMembersByPost(0L, null, 20));
            assertCode(400, () -> directory.findActiveMembersByPost(30L, -1L, 20));
            assertCode(400, () -> directory.findActiveMembersByPost(30L, null, 0));
            assertCode(400, () -> directory.findActiveMembersByPost(30L, null, 201));
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void postLookupUsesCurrentTenantMasterAndRestoresContextOnSuccessAndFailure() {
        TenantMember member = new TenantMember(101L, "alice", "Alice");
        when(mapper.selectActiveMembersByPost(2L, 30L, 100L, 20)).thenAnswer(call -> {
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("master");
            assertThat(DataScopeContextHolder.isSkip()).isTrue();
            return List.of(member);
        });
        when(mapper.selectActiveMembersByPost(2L, 31L, null, 20))
                .thenThrow(new IllegalStateException("database unavailable"));
        DynamicDataSourceContextHolder.push("business-db");
        try (MockedStatic<SessionHelper> ignored = login()) {
            List<TenantMember> result = directory.findActiveMembersByPost(30L, 100L, 20);
            assertThat(result).containsExactly(member);
            assertThatThrownBy(() -> result.add(member)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
            assertThat(DataScopeContextHolder.isSkip()).isFalse();
            assertThatThrownBy(() -> directory.findActiveMembersByPost(31L, null, 20))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("business-db");
            assertThat(DataScopeContextHolder.isSkip()).isFalse();
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
