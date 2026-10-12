package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessObjectMapper extends BaseMapper<AiBusinessObject> {

    Page<BusinessObjectVO> selectObjectPage(Page<BusinessObjectVO> page,
                                            @Param("tenantId") Long tenantId,
                                            @Param("query") BusinessObjectQueryDTO query);

    List<BusinessObjectVO> selectObjectList(@Param("tenantId") Long tenantId,
                                            @Param("query") BusinessObjectQueryDTO query);

    BusinessObjectVO selectObjectDetail(@Param("tenantId") Long tenantId,
                                        @Param("id") Long id);

    BusinessObjectVO selectObjectDetailByCode(@Param("tenantId") Long tenantId,
                                              @Param("suiteCode") String suiteCode,
                                              @Param("objectCode") String objectCode);

    AiBusinessObject selectByObjectCode(@Param("tenantId") Long tenantId,
                                        @Param("suiteCode") String suiteCode,
                                        @Param("objectCode") String objectCode);

    AiBusinessObject selectFirstByObjectCode(@Param("tenantId") Long tenantId,
                                             @Param("objectCode") String objectCode);

    AiBusinessObject selectByConfigKey(@Param("tenantId") Long tenantId,
                                       @Param("configKey") String configKey);

    Long countByObjectCode(@Param("tenantId") Long tenantId,
                           @Param("suiteCode") String suiteCode,
                           @Param("objectCode") String objectCode,
                           @Param("excludeId") Long excludeId);

    Long countRelationsByObject(@Param("tenantId") Long tenantId,
                                @Param("suiteCode") String suiteCode,
                                @Param("objectCode") String objectCode);

    Long countAppsByObject(@Param("tenantId") Long tenantId,
                           @Param("suiteCode") String suiteCode,
                           @Param("objectCode") String objectCode);

    Long countBindingsByObject(@Param("tenantId") Long tenantId,
                               @Param("suiteCode") String suiteCode,
                               @Param("objectCode") String objectCode);

    int updateAppSuiteByObject(@Param("tenantId") Long tenantId,
                               @Param("oldSuiteCode") String oldSuiteCode,
                               @Param("newSuiteCode") String newSuiteCode,
                               @Param("objectCode") String objectCode);

    int updateDesignVersionSuiteByObject(@Param("tenantId") Long tenantId,
                                         @Param("objectId") Long objectId,
                                         @Param("oldSuiteCode") String oldSuiteCode,
                                         @Param("newSuiteCode") String newSuiteCode);

    int updateTriggerSuiteByObject(@Param("tenantId") Long tenantId,
                                   @Param("oldSuiteCode") String oldSuiteCode,
                                   @Param("newSuiteCode") String newSuiteCode,
                                   @Param("objectCode") String objectCode);

    int updateDocumentConfigSuiteByObject(@Param("tenantId") Long tenantId,
                                          @Param("objectId") Long objectId,
                                          @Param("oldSuiteCode") String oldSuiteCode,
                                          @Param("newSuiteCode") String newSuiteCode,
                                          @Param("objectCode") String objectCode);

    List<AiBusinessObject> selectBySuiteCode(@Param("tenantId") Long tenantId,
                                             @Param("suiteCode") String suiteCode);
}
