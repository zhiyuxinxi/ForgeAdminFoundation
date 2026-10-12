package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户-租户成员关系表实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_tenant")
public class SysUserTenant extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 成员类型（1-租户管理员，2-普通成员）
     */
    private Integer memberType;

    /**
     * 是否默认租户（0-否，1-是）
     */
    private Integer isDefault;

    /**
     * 状态（0-禁用，1-正常）
     */
    private Integer status;
}
