package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.auth.config.StpInterfaceImpl;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.support.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Uses real Sa-Token token sessions/permission consumer and Spring transaction synchronization; DB writes are mocked. */
class UserSessionRevocationTest {
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final SysUserTenantMapper members = mock(SysUserTenantMapper.class);
    private final SysTenantMapper tenants = mock(SysTenantMapper.class);
    private final SysUserTenant member = new SysUserTenant();
    private final TestTransactions transactions = new TestTransactions();
    private final TransactionTemplate transaction = new TransactionTemplate(transactions);
    private final SysUserServiceImpl service = new SysUserServiceImpl(users,
            mock(SysUserRoleMapper.class), mock(SysUserOrgRoleMapper.class), mock(SysUserOrgMapper.class),
            mock(SysUserPostMapper.class), members, tenants, mock(SysRoleMapper.class), mock(SysRoleOrgMapper.class),
            mock(SysOrgMapper.class), mock(SysPostMapper.class), mock(SysRegionMapper.class), mock(IUserLoadService.class));
    private StpLogic previousLogic;
    private SaTokenContext previousContext;
    private StpInterface previousPermissions;
    private SaTokenDao previousDao;
    private SaTokenDaoDefaultImpl tokenDao;
    private StpLogic logic;
    private MockMvc http;
    private String caller;
    private String targetPc;
    private String targetApp;
    private String otherTenant;
    private String otherUser;

    @BeforeAll
    static void initializeMetadata() {
        for (Class<?> entity : new Class<?>[]{SysUser.class, SysUserTenant.class, SysTenant.class}) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "auth010"), entity);
        }
    }

    @BeforeEach
    void setUp() {
        previousLogic = StpUtil.getStpLogic();
        previousContext = SaManager.getSaTokenContext();
        previousPermissions = SaManager.getStpInterface();
        previousDao = SaManager.getSaTokenDao();
        tokenDao = new SaTokenDaoDefaultImpl();
        SaManager.setSaTokenDao(tokenDao);
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
        SaManager.setStpInterface(new StpInterfaceImpl());
        logic = new StpLogic("auth010-" + UUID.randomUUID()).setConfig(new SaTokenConfig()
                .setTokenName("satoken").setIsConcurrent(true).setIsShare(false).setIsReadCookie(false));
        StpUtil.setStpLogic(logic);
        http = MockMvcBuilders.standaloneSetup(new ProtectedEndpoint()).build();
        bind(null);
        caller = token(100L, 7L, 0, "pc");
        targetPc = token(200L, 7L, 2, "pc");
        targetApp = token(200L, 7L, 2, "app");
        otherTenant = token(200L, 99L, 2, "pc");
        otherUser = token(300L, 7L, 2, "pc");
        bind(caller);
        SysUser target = new SysUser();
        target.setId(200L);
        target.setUserType(2);
        when(users.selectById(200L)).thenReturn(target);
        when(users.updateById(any(SysUser.class))).thenReturn(1);
        member.setUserId(200L);
        member.setTenantId(7L);
        member.setMemberType(2);
        member.setStatus(1);
        when(members.selectCount(any())).thenAnswer(call -> visible(call.getArgument(0)) ? 1L : 0L);
        when(members.selectOne(any())).thenAnswer(call -> visible(call.getArgument(0)) ? member : null);
        when(members.update(any(SysUserTenant.class), any(Wrapper.class))).thenAnswer(call -> {
            member.setStatus(call.getArgument(0, SysUserTenant.class).getStatus());
            return 1;
        });
    }

    @AfterEach
    void restoreGlobals() {
        RequestContextHolder.resetRequestAttributes();
        StpUtil.setStpLogic(previousLogic);
        SaManager.setSaTokenContext(previousContext);
        SaManager.setStpInterface(previousPermissions);
        SaManager.setSaTokenDao(previousDao);
        tokenDao.destroy();
    }

    @Test
    void globalDisableRevokesAllTargetClientsOnlyAfterCommit() throws Exception {
        access(targetPc, 200);
        bind(caller);
        transaction.executeWithoutResult(state -> {
            assertThat(service.updateUserStatus(200L, 0)).isTrue();
            assertThat(logic.getLoginIdByToken(targetPc)).isNotNull();
            assertThat(logic.getLoginIdByToken(targetApp)).isNotNull();
        });
        access(targetPc, 401);
        access(targetApp, 401);
        access(otherTenant, 401);
        access(otherUser, 200);
        access(caller, 200);
        assertThat(transactions.commits).isEqualTo(1);
    }

    @Test
    void administrativePasswordResetRevokesOldTokensAndPersistsEncryptedPassword() throws Exception {
        transaction.executeWithoutResult(state -> assertThat(service.resetPassword(200L, "Replacement!234")).isTrue());
        ArgumentCaptor<SysUser> changed = ArgumentCaptor.forClass(SysUser.class);
        verify(users).updateById(changed.capture());
        assertThat(PasswordUtil.matches("Replacement!234", changed.getValue().getPassword())).isTrue();
        assertThat(changed.getValue().getForcePasswordChange()).isTrue();
        access(targetPc, 401);
        access(targetApp, 401);
        access(otherTenant, 401);
        access(otherUser, 200);
    }

    @Test
    void membershipDisableRevokesOnlyCurrentTenantAndCanRetryForDisabledMember() throws Exception {
        makeTenantManager();
        transaction.executeWithoutResult(state -> assertThat(service.updateUserStatus(200L, 0)).isTrue());
        access(targetPc, 401);
        access(targetApp, 401);
        access(otherTenant, 200);
        access(otherUser, 200);
        bind(caller);
        assertThat(member.getStatus()).isZero();
        transaction.executeWithoutResult(state -> assertThat(service.updateUserStatus(200L, 0)).isTrue());
        transaction.executeWithoutResult(state -> assertThat(service.updateUserStatus(200L, 1)).isTrue());
        assertThat(member.getStatus()).isEqualTo(1);
        access(otherTenant, 200);
    }

    @Test
    void rollbackDoesNotRevokeExistingAuthorization() throws Exception {
        transaction.executeWithoutResult(state -> {
            assertThat(service.updateUserStatus(200L, 0)).isTrue();
            state.setRollbackOnly();
        });
        assertThat(transactions.commits).isZero();
        assertThat(transactions.rollbacks).isEqualTo(1);
        access(targetPc, 200);
        access(targetApp, 200);
        access(otherTenant, 200);
    }

    @Test
    void failedWriteAndReenablingDoNotRevokeSessions() throws Exception {
        when(users.updateById(any(SysUser.class))).thenReturn(0);
        transaction.executeWithoutResult(state -> assertThat(service.updateUserStatus(200L, 0)).isFalse());
        transaction.executeWithoutResult(state -> assertThat(service.resetPassword(200L, "Replacement!234")).isFalse());
        when(users.updateById(any(SysUser.class))).thenReturn(1);
        transaction.executeWithoutResult(state -> assertThat(service.updateUserStatus(200L, 1)).isTrue());
        access(targetPc, 200);
        access(targetApp, 200);
    }

    @Test
    void revocationFailureIsReportedAfterCommitAndRetryCanRevoke() throws Exception {
        StpLogic failing = spy(logic);
        doThrow(new IllegalStateException("controlled session store failure")).when(failing).kickout(200L);
        StpUtil.setStpLogic(failing);
        assertThatThrownBy(() -> transaction.executeWithoutResult(state -> service.updateUserStatus(200L, 0)))
                .hasMessage("controlled session store failure");
        assertThat(transactions.commits).isEqualTo(1);
        access(targetPc, 200);
        StpUtil.setStpLogic(logic);
        bind(caller);
        transaction.executeWithoutResult(state -> assertThat(service.updateUserStatus(200L, 0)).isTrue());
        access(targetPc, 401);
    }

    @Test
    void expandedStatusTargetLookupStillRejectsOtherTenantAndPrivilegedMembers() throws Exception {
        makeTenantManager();
        doReturn(0L).when(members).selectCount(any());
        assertThatThrownBy(() -> service.updateUserStatus(200L, 0)).hasMessage("无权操作非本租户用户");
        doReturn(1L).when(members).selectCount(any());
        member.setMemberType(1);
        assertThatThrownBy(() -> service.updateUserStatus(200L, 0)).hasMessage("租户管理员只能维护普通用户");
        verify(members, never()).update(any(SysUserTenant.class), any(Wrapper.class));
        access(targetPc, 200);
    }

    @Test
    void publicMutationMethodsDeclareTransactionBoundary() throws Exception {
        var attributes = new AnnotationTransactionAttributeSource();
        assertThat(attributes.getTransactionAttribute(SysUserServiceImpl.class.getMethod("updateUserStatus",
                Long.class, Integer.class), SysUserServiceImpl.class)).isNotNull();
        assertThat(attributes.getTransactionAttribute(SysUserServiceImpl.class.getMethod("resetPassword",
                Long.class, String.class), SysUserServiceImpl.class)).isNotNull();
    }

    private boolean visible(Wrapper<?> query) {
        return member.getStatus() == 1 || !query.getSqlSegment().contains("status");
    }

    private void makeTenantManager() {
        LoginUser user = (LoginUser) logic.getTokenSessionByToken(caller).get("loginUser");
        user.setUserType(1);
        logic.getTokenSessionByToken(caller).set("loginUser", user);
    }

    private String token(long userId, long tenantId, int type, String device) {
        String token = logic.createLoginSession(userId, new SaLoginModel().setDevice(device));
        LoginUser snapshot = new LoginUser();
        snapshot.setUserId(userId);
        snapshot.setTenantId(tenantId);
        snapshot.setUserType(type);
        snapshot.setPermissions(Set.of("system:user:list"));
        logic.getTokenSessionByToken(token).set("loginUser", snapshot);
        return token;
    }

    private void bind(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader("satoken", token);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void access(String token, int expectedStatus) throws Exception {
        http.perform(get("/auth010/protected").header("satoken", token)).andExpect(status().is(expectedStatus));
    }

    @RestController
    static class ProtectedEndpoint {
        @GetMapping("/auth010/protected")
        String read() {
            StpUtil.checkLogin();
            StpUtil.checkPermission("system:user:list");
            return "ok";
        }

        @ExceptionHandler(NotLoginException.class)
        @ResponseStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
        void denied() {
        }
    }

    static class TestTransactions extends AbstractPlatformTransactionManager {
        int commits;
        int rollbacks;

        @Override
        protected Object doGetTransaction() { return new Object(); }

        @Override
        protected void doBegin(Object resource, TransactionDefinition definition) { }

        @Override
        protected void doCommit(DefaultTransactionStatus state) { commits++; }

        @Override
        protected void doRollback(DefaultTransactionStatus state) { rollbacks++; }
    }
}
