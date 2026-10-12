package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageTemplate;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.starter.datascope.config.DataScopeQuerySource;
import org.apache.ibatis.annotations.Param;

public interface SysMessageTemplateMapper extends BaseMapper<SysMessageTemplate> {
    @DataScopeQuerySource("selectCount")
    Long countByCode(@Param("code") String code, @Param("excludedId") Long excludedId);

    @DataScopeQuerySource("selectList")
    SysMessageTemplate selectByCode(@Param("code") String code);

    @DataScopeQuerySource("selectList")
    IPage<SysMessageTemplate> selectTemplatePage(IPage<SysMessageTemplate> page,
            @Param("type") String type, @Param("keyword") String keyword);
}
