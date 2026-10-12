package com.mdframe.forge.starter.tenant.datasource;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 租户业务数据源路由配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "forge.business.datasource")
public class BusinessDataSourceProperties {

    /**
     * 是否启用业务侧租户数据源能力。
     */
    private boolean enabled = false;

    /**
     * sys_config 未配置时是否默认启用租户路由。
     */
    private boolean tenantRoutingEnabledDefault = false;

}
