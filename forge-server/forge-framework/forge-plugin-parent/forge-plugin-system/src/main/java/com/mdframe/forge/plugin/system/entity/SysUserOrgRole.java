package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户组织内角色授权。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_org_role")
public class SysUserOrgRole extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 组织ID。
     */
    private Long orgId;

    /**
     * 角色ID。
     */
    private Long roleId;
}
