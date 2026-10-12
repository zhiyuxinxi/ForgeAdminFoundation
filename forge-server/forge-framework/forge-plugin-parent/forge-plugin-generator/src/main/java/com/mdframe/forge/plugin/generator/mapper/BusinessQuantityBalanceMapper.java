package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessQuantityBalance;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessQuantityQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessQuantityBalanceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BusinessQuantityBalanceMapper extends BaseMapper<AiBusinessQuantityBalance> {

    Page<BusinessQuantityBalanceVO> selectBalancePage(Page<BusinessQuantityBalanceVO> page,
                                                      @Param("tenantId") Long tenantId,
                                                      @Param("query") BusinessQuantityQueryDTO query);

    AiBusinessQuantityBalance selectByKey(@Param("tenantId") Long tenantId,
                                          @Param("accountCode") String accountCode,
                                          @Param("itemCode") String itemCode,
                                          @Param("dimensionKey") String dimensionKey);

    int increaseQuantity(@Param("tenantId") Long tenantId,
                         @Param("accountCode") String accountCode,
                         @Param("itemCode") String itemCode,
                         @Param("dimensionKey") String dimensionKey,
                         @Param("quantity") Long quantity);

    int decreaseAvailableQuantity(@Param("tenantId") Long tenantId,
                                  @Param("accountCode") String accountCode,
                                  @Param("itemCode") String itemCode,
                                  @Param("dimensionKey") String dimensionKey,
                                  @Param("quantity") Long quantity);

    int lockQuantity(@Param("tenantId") Long tenantId,
                     @Param("accountCode") String accountCode,
                     @Param("itemCode") String itemCode,
                     @Param("dimensionKey") String dimensionKey,
                     @Param("quantity") Long quantity);

    int releaseLockedQuantity(@Param("tenantId") Long tenantId,
                              @Param("accountCode") String accountCode,
                              @Param("itemCode") String itemCode,
                              @Param("dimensionKey") String dimensionKey,
                              @Param("quantity") Long quantity);

    int commitLockedQuantity(@Param("tenantId") Long tenantId,
                             @Param("accountCode") String accountCode,
                             @Param("itemCode") String itemCode,
                             @Param("dimensionKey") String dimensionKey,
                             @Param("quantity") Long quantity);
}
