package com.mdframe.forge.business.core.demo.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户业务数据源路由演示记录。
 */
@Data
public class BusinessDatasourceDemoRecord {

    private Long id;

    private Long tenantId;

    private String title;

    private String routeKey;

    private Long createBy;

    private LocalDateTime createTime;

    private Long createDept;

    private Long updateBy;

    private LocalDateTime updateTime;
}
