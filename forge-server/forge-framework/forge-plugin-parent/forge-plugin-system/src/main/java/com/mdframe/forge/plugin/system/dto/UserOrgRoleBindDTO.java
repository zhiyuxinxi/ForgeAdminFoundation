package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户组织角色绑定 DTO。
 */
@Data
public class UserOrgRoleBindDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 组织ID。
     */
    private Long orgId;

    /**
     * 角色ID列表。
     */
    private List<Long> roleIds;

    /**
     * 操作租户ID。
     */
    private Long tenantId;
}
