package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色适用组织范围。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_org")
public class SysRoleOrg extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID。
     */
    private Long roleId;

    /**
     * 组织ID。
     */
    private Long orgId;
}
