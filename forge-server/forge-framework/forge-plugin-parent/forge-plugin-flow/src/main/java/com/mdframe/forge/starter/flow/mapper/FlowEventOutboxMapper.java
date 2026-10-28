package com.mdframe.forge.starter.flow.mapper;

import com.mdframe.forge.starter.flow.event.FlowEventOutboxEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FlowEventOutboxMapper {
    int insert(FlowEventOutboxEntry entry);

    FlowEventOutboxEntry selectRecorded(@Param("tenantId") Long tenantId, @Param("eventId") String eventId);

    FlowEventOutboxEntry selectTerminal(@Param("tenantId") Long tenantId,
                                      @Param("processInstanceId") String processInstanceId,
                                      @Param("eventType") String eventType);
}
