package com.mdframe.forge.plugin.ai.agenttool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.ai.agenttool.domain.AiAgentToolPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiAgentToolPermissionMapper extends BaseMapper<AiAgentToolPermission> {

    List<AiAgentToolPermission> selectByAgentId(@Param("agentId") Long agentId);

    List<AiAgentToolPermission> selectByAgentIdAndToolKey(@Param("agentId") Long agentId,
                                                           @Param("toolKey") String toolKey);

    List<String> selectRuntimeDecisions(@Param("tenantId") Long tenantId,
                                        @Param("userId") Long userId,
                                        @Param("sessionId") String sessionId,
                                        @Param("agentId") Long agentId,
                                        @Param("toolSource") String toolSource,
                                        @Param("toolKey") String toolKey);

    int deleteByAgentIdAndToolKey(@Param("agentId") Long agentId,
                                   @Param("toolKey") String toolKey);
}
