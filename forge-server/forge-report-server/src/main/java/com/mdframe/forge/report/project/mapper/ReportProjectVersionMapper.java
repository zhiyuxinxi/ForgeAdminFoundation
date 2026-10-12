package com.mdframe.forge.report.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.report.project.domain.ReportProjectVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 大屏项目版本 Mapper。
 */
@Mapper
public interface ReportProjectVersionMapper extends BaseMapper<ReportProjectVersion> {

    /**
     * 分页查询项目版本。
     */
    Page<ReportProjectVersion> selectVersionPage(Page<ReportProjectVersion> page,
                                                 @Param("tenantId") Long tenantId,
                                                 @Param("projectId") Long projectId);

    /**
     * 查询项目最大版本号。
     */
    Integer selectMaxVersionNo(@Param("tenantId") Long tenantId,
                               @Param("projectId") Long projectId);
}
