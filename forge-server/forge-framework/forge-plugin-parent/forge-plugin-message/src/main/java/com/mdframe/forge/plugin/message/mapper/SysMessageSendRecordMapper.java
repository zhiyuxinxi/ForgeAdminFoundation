package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageSendRecord;
import com.mdframe.forge.starter.datascope.config.DataScopeQuerySource;
import org.apache.ibatis.annotations.Param;

public interface SysMessageSendRecordMapper extends BaseMapper<SysMessageSendRecord> {
    @DataScopeQuerySource("selectList")
    SysMessageSendRecord selectForMessage(@Param("tenantId") Long tenantId, @Param("messageId") Long messageId);
}
