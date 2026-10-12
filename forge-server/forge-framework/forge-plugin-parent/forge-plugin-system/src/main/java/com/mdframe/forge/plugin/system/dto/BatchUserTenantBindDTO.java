package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量用户租户绑定请求。
 */
@Data
public class BatchUserTenantBindDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID列表。
     */
    private List<Long> userIds;

    /**
     * 目标租户ID。
     */
    private Long tenantId;

    /**
     * 成员类型（1-租户管理员，2-普通成员）。
     */
    private Integer memberType;
}
