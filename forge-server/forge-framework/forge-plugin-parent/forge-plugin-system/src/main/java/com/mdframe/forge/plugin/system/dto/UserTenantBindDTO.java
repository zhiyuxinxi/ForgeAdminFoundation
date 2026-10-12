package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户租户绑定请求
 */
@Data
public class UserTenantBindDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 绑定租户ID列表
     */
    private List<Long> tenantIds;

    /**
     * 默认租户ID
     */
    private Long defaultTenantId;

    /**
     * 成员类型（1-租户管理员，2-普通成员）
     */
    private Integer memberType;
}
