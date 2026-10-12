package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaExecutionLog;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaExecutionLogQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FormulaExecutionLogMapper extends BaseMapper<AiFormulaExecutionLog> {

    Page<AiFormulaExecutionLog> selectFormulaExecutionLogPage(Page<AiFormulaExecutionLog> page,
                                                              @Param("tenantId") Long tenantId,
                                                              @Param("query") FormulaExecutionLogQueryDTO query);

    AiFormulaExecutionLog selectFormulaExecutionLogById(@Param("tenantId") Long tenantId,
                                                        @Param("id") Long id);
}
