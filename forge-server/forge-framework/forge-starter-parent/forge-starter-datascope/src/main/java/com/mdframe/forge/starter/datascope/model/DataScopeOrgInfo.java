package com.mdframe.forge.starter.datascope.model;

import lombok.Data;

/**
 * 数据权限组织层级快照。
 */
@Data
public class DataScopeOrgInfo {

    private Long orgId;

    private String ancestors;
}
