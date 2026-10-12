package com.mdframe.forge.starter.datascope.handler;

import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.config.DataScopeQuerySource;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataScopeQuerySourceTest {
    interface Queries {
        @DataScopeQuerySource("legacy")
        @Select("SELECT id FROM messages")
        Long current();

        @Select("SELECT id FROM messages")
        Long unrelated();
    }

    @Test
    void explicitNewPolicyWinsWithoutReadingTheOldPolicy() throws SQLException {
        IDataScopeService service = mock(IDataScopeService.class);
        when(service.getDataScopeConfig(id("current"))).thenReturn(disabledPolicy());
        invoke(service, "current");
        verify(service, never()).getDataScopeConfig(id("legacy"));
    }

    @Test
    void missingNewPolicyUsesOnlyItsDeclaredSameMapperSource() throws SQLException {
        IDataScopeService service = mock(IDataScopeService.class);
        when(service.getDataScopeConfig(id("legacy"))).thenReturn(disabledPolicy());
        invoke(service, "current");
        verify(service).getDataScopeConfig(id("current"));
        verify(service).getDataScopeConfig(id("legacy"));
        verifyNoMoreInteractions(service);
    }

    @Test
    void unrelatedMethodAndMissingSourceKeepStrictDenial() {
        IDataScopeService service = mock(IDataScopeService.class);
        when(service.getDataScopeConfig(id("legacy"))).thenReturn(disabledPolicy());
        assertThrows(SQLException.class, () -> invoke(service, "unrelated"));
        verify(service, never()).getDataScopeConfig(id("legacy"));
        assertThrows(SQLException.class, () -> invoke(mock(IDataScopeService.class), "current"));
    }

    @Test
    void policyReadFailuresAreNotTreatedAsAbsentConfiguration() {
        IDataScopeService service = mock(IDataScopeService.class);
        when(service.getDataScopeConfig(id("current"))).thenThrow(new IllegalStateException("policy unavailable"));
        assertThrows(IllegalStateException.class, () -> invoke(service, "current"));
        verify(service, never()).getDataScopeConfig(id("legacy"));
        IDataScopeService legacyFailure = mock(IDataScopeService.class);
        when(legacyFailure.getDataScopeConfig(id("legacy"))).thenThrow(new IllegalStateException("policy unavailable"));
        assertThrows(IllegalStateException.class, () -> invoke(legacyFailure, "current"));
    }

    private void invoke(IDataScopeService service, String method) throws SQLException {
        Configuration configuration = new Configuration();
        configuration.addMapper(Queries.class);
        MappedStatement statement = configuration.getMappedStatement(id(method));
        DataScopeProperties properties = new DataScopeProperties();
        properties.setUnconfiguredPolicy(DataScopeProperties.UnconfiguredPolicy.DENY);
        new DataScopeInterceptor(service, properties).beforeQuery(null, statement, null,
                RowBounds.DEFAULT, null, statement.getBoundSql(null));
    }

    private String id(String method) { return Queries.class.getName() + "." + method; }

    private SysDataScopeConfig disabledPolicy() {
        SysDataScopeConfig config = new SysDataScopeConfig();
        config.setEnabled(0);
        return config;
    }
}
