package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量用户角色绑定请求。
 */
@Data
public class BatchUserRoleBindDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID列表。
     */
    private List<Long> userIds;

    /**
     * 角色ID列表。
     */
    private List<Long> roleIds;

    /**
     * 操作租户ID。
     */
    private Long tenantId;
}
