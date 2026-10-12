package com.mdframe.forge.starter.datascope.model;

import lombok.Data;

/**
 * 数据权限角色范围快照。
 */
@Data
public class DataScopeRoleInfo {

    private Long roleId;

    private Integer dataScope;
}
