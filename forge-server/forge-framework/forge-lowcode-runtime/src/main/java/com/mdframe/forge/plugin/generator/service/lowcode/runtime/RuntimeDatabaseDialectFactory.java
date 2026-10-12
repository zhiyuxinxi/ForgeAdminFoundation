package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 低代码运行时数据库方言工厂。
 */
@Component
@RequiredArgsConstructor
public class RuntimeDatabaseDialectFactory {

    private static final String DEFAULT_DB_TYPE = "MySQL";

    private final List<RuntimeDatabaseDialect> dialects;

    public RuntimeDatabaseDialect resolve(LowcodeRuntimeDataSourceContext context) {
        return resolve(context == null ? null : context.getDbType());
    }

    public RuntimeDatabaseDialect resolve(String dbType) {
        String effectiveDbType = StringUtils.defaultIfBlank(dbType, DEFAULT_DB_TYPE);
        return dialects.stream()
            .filter(dialect -> dialect.supports(effectiveDbType))
            .findFirst()
            .orElseThrow(() -> new BusinessException(UiMessages.translate("暂不支持的运行数据源数据库类型: ") + effectiveDbType));
    }
}
