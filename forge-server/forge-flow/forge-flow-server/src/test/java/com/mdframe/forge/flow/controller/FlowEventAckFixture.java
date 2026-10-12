package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.flow.event.FlowEventAcknowledgementService;
import com.mdframe.forge.starter.flow.event.FlowEventDeliveryIdentity;
import com.mdframe.forge.starter.flow.event.FlowEventDeliveryProperties;
import com.mdframe.forge.starter.flow.mapper.FlowEventDeliveryMapper;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class FlowEventAckFixture implements AutoCloseable {
    final int previousRefreshPeriod = disableBackgroundRefresh();
    final SaTokenDao previousDao = SaManager.getSaTokenDao();
    final SaTokenContext previousContext = SaManager.getSaTokenContext();
    final StpLogic previousLogic = StpUtil.getStpLogic();
    final StpInterface previousPermissions = SaManager.getStpInterface();
    final SaTokenDaoDefaultImpl dao = new SaTokenDaoDefaultImpl();
    final StpLogic logic = new StpLogic("login").setConfig(new SaTokenConfig().setTimeout(3600)
            .setTokenName("Authorization").setTokenPrefix("Bearer").setIsShare(false));
    final DriverManagerDataSource source = new DriverManagerDataSource(
            "jdbc:h2:mem:ack" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    final JdbcTemplate jdbc = new JdbcTemplate(source);
    final FlowEventDeliveryProperties properties = new FlowEventDeliveryProperties();
    final FlowEventDeliveryMapper mapper;
    final FlowEventAcknowledgementController controller;
    boolean permission = true;

    FlowEventAckFixture() throws Exception {
        String sql = Files.readString(Path.of("forge-server/db/migration/"
                + "V1.0.2026100911__flow_event_delivery_confirmation.sql"));
        jdbc.execute(sql.substring(0, sql.indexOf(';')).replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", ""));
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(new MybatisConfiguration());
        factory.setMapperLocations(new FileSystemResource("forge-server/forge-framework/forge-plugin-parent/"
                + "forge-plugin-flow/src/main/resources/mapper/FlowEventDeliveryMapper.xml"));
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(FlowEventDeliveryMapper.class);
        properties.setEnabled(true);
        var binding = new FlowEventDeliveryProperties.Consumer();
        binding.setId("business-admin");
        binding.setLoginId("501");
        binding.setTenantId(97L);
        binding.setModelKeys(List.of("approval"));
        binding.setDestinationType("redis");
        binding.setDestination("flow:event:all");
        properties.setConsumers(List.of(binding));
        ProxyFactory proxy = new ProxyFactory(new FlowEventAcknowledgementService(properties, mapper));
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source),
                new AnnotationTransactionAttributeSource()));
        controller = new FlowEventAcknowledgementController((FlowEventAcknowledgementService) proxy.getProxy());
        SaManager.setSaTokenDao(dao);
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        StpUtil.setStpLogic(logic);
        SaManager.setStpInterface(new StpInterface() {
            public List<String> getPermissionList(Object loginId, String loginType) {
                return permission ? List.of("flow:event:ack") : List.of();
            }
            public List<String> getRoleList(Object loginId, String loginType) {
                return List.of();
            }
        });
    }

    String token(String loginId, long tenant) {
        String token = logic.createLoginSession(loginId, new SaLoginModel());
        LoginUser user = new LoginUser();
        user.setUserId(Long.valueOf(loginId));
        user.setTenantId(tenant);
        logic.getTokenSessionByToken(token, true).set("loginUser", user);
        return token;
    }

    void use(String token) {
        SaRequest request = mock(SaRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        SaStorage storage = mock(SaStorage.class);
        Map<String, Object> values = new HashMap<>();
        when(storage.get(anyString())).thenAnswer(call -> values.get(call.getArgument(0)));
        doAnswer(call -> {
            values.put(call.getArgument(0), call.getArgument(1));
            return storage;
        })
                .when(storage).set(anyString(), any());
        doAnswer(call -> {
            values.remove(call.getArgument(0));
            return storage;
        })
                .when(storage).delete(anyString());
        SaTokenContextForThreadLocalStorage.setBox(request, mock(SaResponse.class), storage);
    }

    void pending(String event, String consumer) {
        mapper.insert(new FlowEventDeliveryIdentity(event, 97L, consumer));
    }

    String status(String event) {
        return jdbc.queryForObject("SELECT status FROM sys_flow_event_delivery WHERE event_id=?", String.class, event);
    }

    private static int disableBackgroundRefresh() {
        int previous = SaManager.getConfig().getDataRefreshPeriod();
        SaManager.getConfig().setDataRefreshPeriod(0);
        return previous;
    }

    @Override
    public void close() {
        SaTokenContextForThreadLocalStorage.clearBox();
        StpUtil.setStpLogic(previousLogic);
        SaManager.setStpInterface(previousPermissions);
        SaManager.setSaTokenContext(previousContext);
        SaManager.setSaTokenDao(previousDao);
        SaManager.getConfig().setDataRefreshPeriod(previousRefreshPeriod);
        dao.refreshFlag = false;
        if (dao.refreshThread != null) {
            dao.refreshThread.interrupt();
        }
        jdbc.execute("SHUTDOWN");
    }
}
