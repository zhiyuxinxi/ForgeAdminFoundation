package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.controller.SysPostController;
import com.mdframe.forge.plugin.system.entity.SysPost;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysPostBatchDeleteTest {
    private final SysPostMapper mapper = mock(SysPostMapper.class);
    private final SysPostServiceImpl service = new SysPostServiceImpl(
            mapper, mock(SysTenantMapper.class), mock(SysOrgMapper.class));
    private final SysPostController controller = new SysPostController(service);

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reportsFailureForMissingOrZeroRowDeletionAndStillProcessesRemainingIds(boolean recordExists) {
        deletablePost(11L);
        if (recordExists) {
            when(mapper.selectById(12L)).thenReturn(post(12L, 7L));
            when(mapper.deleteById(12L)).thenReturn(0);
        }
        deletablePost(13L);

        try (MockedStatic<SessionHelper> ignored = login()) {
            assertThat(controller.removeBatch(new Long[]{11L, 12L, 13L}).getCode()).isEqualTo(500);
        }

        verify(mapper).deleteById(11L);
        verify(mapper).deleteById(13L);
        if (recordExists) {
            verify(mapper).deleteById(12L);
        } else {
            verify(mapper, never()).deleteById(12L);
        }
    }

    @Test
    void reportsSuccessWhenEveryPostWasDeleted() {
        deletablePost(11L);
        deletablePost(12L);

        try (MockedStatic<SessionHelper> ignored = login()) {
            assertThat(controller.removeBatch(new Long[]{11L, 12L}).getCode()).isEqualTo(200);
        }

        verify(mapper).deleteById(11L);
        verify(mapper).deleteById(12L);
    }

    @Test
    void tenantDenialStillPropagatesWithoutDeletingTheUnauthorizedPost() {
        when(mapper.selectById(11L)).thenReturn(post(11L, 8L));

        try (MockedStatic<SessionHelper> ignored = login()) {
            assertThatThrownBy(() -> controller.removeBatch(new Long[]{11L, 12L}))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("无权操作非本租户岗位");
        }

        verify(mapper, never()).deleteById(11L);
        verify(mapper, never()).selectById(12L);
    }

    @Test
    void persistenceFailureStillPropagatesWithoutBecomingABooleanResult() {
        when(mapper.selectById(11L)).thenReturn(post(11L, 7L));
        DataIntegrityViolationException failure = new DataIntegrityViolationException("referenced post");
        when(mapper.deleteById(11L)).thenThrow(failure);

        try (MockedStatic<SessionHelper> ignored = login()) {
            assertThatThrownBy(() -> controller.removeBatch(new Long[]{11L, 12L})).isSameAs(failure);
        }

        verify(mapper, never()).selectById(12L);
    }

    private void deletablePost(Long id) {
        when(mapper.selectById(id)).thenReturn(post(id, 7L));
        when(mapper.deleteById(id)).thenReturn(1);
    }

    private SysPost post(Long id, Long tenantId) {
        SysPost post = new SysPost();
        post.setId(id);
        post.setTenantId(tenantId);
        return post;
    }

    private MockedStatic<SessionHelper> login() {
        LoginUser user = new LoginUser();
        user.setUserId(9L);
        user.setTenantId(7L);
        user.setUserType(2);
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(user);
        return session;
    }
}
