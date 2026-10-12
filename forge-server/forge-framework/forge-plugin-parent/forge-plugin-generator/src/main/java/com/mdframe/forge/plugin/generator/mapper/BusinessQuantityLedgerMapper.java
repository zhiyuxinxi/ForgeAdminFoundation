package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessQuantityLedger;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessQuantityQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessQuantityLedgerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessQuantityLedgerMapper extends BaseMapper<AiBusinessQuantityLedger> {

    Page<BusinessQuantityLedgerVO> selectLedgerPage(Page<BusinessQuantityLedgerVO> page,
                                                    @Param("tenantId") Long tenantId,
                                                    @Param("query") BusinessQuantityQueryDTO query);

    AiBusinessQuantityLedger selectByIdempotencyKey(@Param("tenantId") Long tenantId,
                                                    @Param("idempotencyKey") String idempotencyKey);
}
