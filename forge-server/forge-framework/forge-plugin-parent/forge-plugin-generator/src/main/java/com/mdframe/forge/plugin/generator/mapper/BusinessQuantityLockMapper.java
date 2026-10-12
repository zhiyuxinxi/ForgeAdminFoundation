package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessQuantityLock;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessQuantityQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessQuantityLockVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessQuantityLockMapper extends BaseMapper<AiBusinessQuantityLock> {

    Page<BusinessQuantityLockVO> selectLockPage(Page<BusinessQuantityLockVO> page,
                                                @Param("tenantId") Long tenantId,
                                                @Param("query") BusinessQuantityQueryDTO query);

    AiBusinessQuantityLock selectByIdempotencyKey(@Param("tenantId") Long tenantId,
                                                 @Param("idempotencyKey") String idempotencyKey);

    AiBusinessQuantityLock selectByLockCode(@Param("tenantId") Long tenantId,
                                            @Param("lockCode") String lockCode);

    AiBusinessQuantityLock selectActiveBySource(@Param("tenantId") Long tenantId,
                                                @Param("accountCode") String accountCode,
                                                @Param("itemCode") String itemCode,
                                                @Param("dimensionKey") String dimensionKey,
                                                @Param("sourceObjectCode") String sourceObjectCode,
                                                @Param("sourceRecordId") String sourceRecordId,
                                                @Param("sourceDetailId") String sourceDetailId);

    int releaseLockQuantity(@Param("tenantId") Long tenantId,
                            @Param("lockId") Long lockId,
                            @Param("quantity") Long quantity);

    int commitLockQuantity(@Param("tenantId") Long tenantId,
                           @Param("lockId") Long lockId,
                           @Param("quantity") Long quantity);

    int updateLockStatus(@Param("tenantId") Long tenantId,
                         @Param("lockId") Long lockId,
                         @Param("lockStatus") String lockStatus);
}
