package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.message.domain.dto.MessageManageQueryDTO;
import com.mdframe.forge.starter.datascope.config.DataScopeQuerySource;
import java.time.LocalDateTime;

public interface SysMessageMapper extends BaseMapper<SysMessage> {

    @DataScopeQuerySource("selectList")
    Page<SysMessage> selectManagementPage(Page<SysMessage> page, @Param("tenantId") Long tenantId,
            @Param("query") MessageManageQueryDTO query, @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    SysMessage selectByBizTypeAndBizKey(@Param("bizType") String bizType,
                                        @Param("bizKey") String bizKey);

    SysMessage selectByIdempotencyKey(@Param("tenantId") Long tenantId,
                                      @Param("idempotencyKey") String idempotencyKey);
}
