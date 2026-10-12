package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 资源批量迁移请求。
 */
@Data
public class SysResourceBatchMigrateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 资源ID列表。
     */
    private List<Long> ids;

    /**
     * 目标父级资源ID，0 表示顶级资源。
     */
    private Long parentId;
}
