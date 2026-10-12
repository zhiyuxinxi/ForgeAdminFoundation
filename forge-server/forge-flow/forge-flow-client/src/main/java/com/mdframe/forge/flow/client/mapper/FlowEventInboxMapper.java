package com.mdframe.forge.flow.client.mapper;

import com.mdframe.forge.flow.client.event.FlowEventInboxEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FlowEventInboxMapper {
    int reserve(FlowEventInboxEntry entry);

    FlowEventInboxEntry selectForUpdate(FlowEventInboxEntry entry);

    int complete(FlowEventInboxEntry entry);
}
