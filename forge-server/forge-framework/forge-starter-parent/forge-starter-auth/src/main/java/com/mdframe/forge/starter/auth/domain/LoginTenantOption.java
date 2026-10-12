package com.mdframe.forge.starter.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录页租户选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginTenantOption implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID。
     */
    private Long tenantId;

    /**
     * 租户名称。
     */
    private String tenantName;

    /**
     * 系统名称。
     */
    private String systemName;

    /**
     * 浏览器标题。
     */
    private String browserTitle;
}
