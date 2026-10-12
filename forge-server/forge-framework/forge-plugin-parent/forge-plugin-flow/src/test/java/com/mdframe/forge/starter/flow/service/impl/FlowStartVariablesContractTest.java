package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlowStartVariablesContractTest {
    private final RuntimeService runtime = mock(RuntimeService.class);
    private final FlowBusinessMapper businesses = mock(FlowBusinessMapper.class);
    private final FlowOrgIntegrationService organizations = mock(FlowOrgIntegrationService.class);
    private final ISysUserService users = mock(ISysUserService.class);
    private final FlowInstanceServiceImpl service = createService();

    @Test
    void reservedIdentityAndOrganizationVariablesKeepCallerInputAndPersistBeforeStarting() {
        when(organizations.getUserInfo("45")).thenReturn(Map.of("realName", "Real Name"));
        when(organizations.getLeaderUserIdByLevel("45", 1)).thenReturn("46");
        var user = new SysUser();
        user.setRegionCode("CN-001");
        when(users.selectUserById(45L)).thenReturn(user);
        when(users.selectUserOrgRoleIds(45L, 8L, 7L)).thenReturn(List.of(2L, 3L));
        when(users.selectUserOrgIds(45L)).thenReturn(List.of(8L, 9L));
        Map<String, Object> input = Map.of("initiator", "forged", "businessKey", "existing-alias", "custom", 12);
        try (var session = mockStatic(SessionHelper.class)) {
            var login = new LoginUser();
            login.setActiveOrgId(8L);
            login.setTenantId(7L);
            session.when(SessionHelper::getTenantId).thenReturn(7L);
            session.when(SessionHelper::getLoginUser).thenReturn(login);
            assertEquals("instance", service.startProcess("model", "order:1", "ORDER", "Title", input,
                    "45", "login-name", "8", "Department"));
        }
        var values = ArgumentCaptor.forClass(Map.class);
        var ordered = inOrder(businesses, runtime);
        var stored = ArgumentCaptor.forClass(FlowBusiness.class);
        ordered.verify(businesses).selectByBusinessKeyAndTenantId(7L, "order:1");
        ordered.verify(businesses).insert(stored.capture());
        ordered.verify(runtime).startProcessInstanceByKey(eq("model"), eq("order:1"), values.capture());
        assertEquals("Real Name", stored.getValue().getApplyUserName());
        assertEquals(7L, stored.getValue().getTenantId());
        assertEquals("45", values.getValue().get("initiator"));
        assertEquals("Real Name", values.getValue().get("startUserName"));
        assertEquals("existing-alias", values.getValue().get("businessKey"));
        assertEquals("order:1", values.getValue().get("flowBusinessKey"));
        assertEquals("46", values.getValue().get("initiatorLeader"));
        assertEquals("CN-001", values.getValue().get("regionCode"));
        assertEquals("2,3", values.getValue().get("startUserRoleIds"));
        assertEquals("8,9", values.getValue().get("startUserOrgIds"));
        assertEquals("forged", input.get("initiator"));
        assertEquals(3, input.size());
    }

    @Test
    void optionalIdentityLookupFailuresRetainRequiredStartVariables() {
        when(organizations.getUserInfo("external")).thenThrow(new IllegalStateException("lookup unavailable"));
        when(organizations.getLeaderUserIdByLevel("external", 1)).thenThrow(new IllegalStateException("leader unavailable"));
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(7L);
            assertEquals("instance", service.startProcess("model", "order:2", "Title", Map.of(),
                    "external", " External Name ", "8", "Department"));
        }
        var values = ArgumentCaptor.forClass(Map.class);
        verify(runtime).startProcessInstanceByKey(eq("model"), eq("order:2"), values.capture());
        assertEquals("External Name", values.getValue().get("startUserName"));
        assertEquals("external", values.getValue().get("initiator"));
        assertFalse(values.getValue().containsKey("initiatorLeader"));
        assertFalse(values.getValue().containsKey("startUserRoleIds"));
    }

    private FlowInstanceServiceImpl createService() {
        var engine = mock(ProcessEngine.class);
        var repository = mock(RepositoryService.class);
        var definitions = mock(ProcessDefinitionQuery.class, RETURNS_SELF);
        var definition = mock(ProcessDefinition.class);
        when(engine.getRepositoryService()).thenReturn(repository);
        when(repository.createProcessDefinitionQuery()).thenReturn(definitions);
        when(definitions.singleResult()).thenReturn(definition);
        when(definition.getId()).thenReturn("definition");
        when(definition.getKey()).thenReturn("model");
        when(repository.getBpmnModel("definition")).thenReturn(new BpmnModel());
        var instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("instance");
        when(runtime.startProcessInstanceByKey(anyString(), anyString(), anyMap())).thenReturn(instance);
        var target = new FlowInstanceServiceImpl();
        ReflectionTestUtils.setField(target, "processEngine", engine);
        ReflectionTestUtils.setField(target, "runtimeService", runtime);
        ReflectionTestUtils.setField(target, "flowBusinessMapper", businesses);
        ReflectionTestUtils.setField(target, "flowOrgIntegrationService", organizations);
        ReflectionTestUtils.setField(target, "sysUserService", users);
        return target;
    }
}
