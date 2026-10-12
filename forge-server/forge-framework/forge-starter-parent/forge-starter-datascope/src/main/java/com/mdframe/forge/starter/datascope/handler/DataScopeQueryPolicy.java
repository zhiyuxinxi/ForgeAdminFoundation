package com.mdframe.forge.starter.datascope.handler;

import com.mdframe.forge.starter.datascope.config.DataScopeQuerySource;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.apache.ibatis.mapping.MappedStatement;

/** Owns the explicit policy identity retained by queries moved into named XML statements. */
final class DataScopeQueryPolicy {
    private DataScopeQueryPolicy() { }

    static SysDataScopeConfig resolve(IDataScopeService service, MappedStatement statement, String mapperId) {
        SysDataScopeConfig configured = service.getDataScopeConfig(mapperId);
        if (configured != null) {
            return configured;
        }
        String sourceId = queryPolicySource(statement, mapperId);
        return sourceId == null ? null : service.getDataScopeConfig(sourceId);
    }

    private static String queryPolicySource(MappedStatement statement, String mapperId) {
        int separator = mapperId.lastIndexOf('.');
        if (separator < 0) {
            return null;
        }
        String namespace = mapperId.substring(0, separator);
        String name = mapperId.substring(separator + 1);
        Class<?> mapper;
        try {
            mapper = org.apache.ibatis.io.Resources.classForName(namespace);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
        if (!statement.getConfiguration().hasMapper(mapper)) {
            return null;
        }
        for (java.lang.reflect.Method method : mapper.getDeclaredMethods()) {
            DataScopeQuerySource source = method.getAnnotation(DataScopeQuerySource.class);
            if (method.getName().equals(name) && source != null) {
                if (!source.value().matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
                    throw new IllegalStateException("Data-scope query source must be a same-mapper method");
                }
                return namespace + "." + source.value();
            }
        }
        return null;
    }

}
