package com.mdframe.forge.plugin.ai.dashboard.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 大屏组件数据资产血缘。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_dashboard_component_lineage")
public class AiDashboardComponentLineage extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long recordId;

    private Long userId;

    private String sessionId;

    private Long projectId;

    private String projectName;

    private Long businessDefinitionId;

    private String businessName;

    private Integer componentIndex;

    private String componentKey;

    private String componentTitle;

    private Long datasetId;

    private String datasetName;

    private String fieldNames;

    private String bindingStatus;
}
