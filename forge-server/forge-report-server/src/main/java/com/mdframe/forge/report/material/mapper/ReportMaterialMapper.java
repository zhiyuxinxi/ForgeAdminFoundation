package com.mdframe.forge.report.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.report.material.domain.ReportMaterial;
import com.mdframe.forge.report.material.vo.ReportMaterialVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 报表素材 Mapper。
 */
@Mapper
public interface ReportMaterialMapper extends BaseMapper<ReportMaterial> {

    /**
     * 分页查询素材。
     */
    Page<ReportMaterialVO> selectMaterialPage(Page<ReportMaterialVO> page,
                                              @Param("tenantId") Long tenantId,
                                              @Param("materialCategory") String materialCategory,
                                              @Param("originalName") String originalName,
                                              @Param("mimeType") String mimeType,
                                              @Param("isPrivate") Boolean isPrivate,
                                              @Param("currentUserId") Long currentUserId,
                                              @Param("admin") Boolean admin);

    /**
     * 根据文件ID查询素材视图。
     */
    ReportMaterialVO selectMaterialByFileId(@Param("tenantId") Long tenantId,
                                            @Param("fileId") String fileId);

    /**
     * 根据文件ID查询素材。
     */
    ReportMaterial selectActiveByFileId(@Param("tenantId") Long tenantId,
                                        @Param("fileId") String fileId);

    /**
     * 逻辑删除素材。
     */
    int markDeletedByFileId(@Param("tenantId") Long tenantId,
                            @Param("fileId") String fileId);
}
