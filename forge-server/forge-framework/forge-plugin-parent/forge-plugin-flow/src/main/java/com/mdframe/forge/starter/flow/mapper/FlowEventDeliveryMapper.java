package com.mdframe.forge.starter.flow.mapper;

import com.mdframe.forge.starter.flow.event.FlowEventDeliveryIdentity;
import com.mdframe.forge.starter.flow.event.FlowPendingDelivery;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FlowEventDeliveryMapper {
    int insert(FlowEventDeliveryIdentity identity);

    String selectStatus(FlowEventDeliveryIdentity identity);

    String selectBoundConsumer(@Param("eventId") String eventId, @Param("tenantId") Long tenantId);

    int acknowledge(FlowEventDeliveryIdentity identity);

    @InterceptorIgnore(tenantLine = "true")
    List<FlowPendingDelivery> selectDue(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int claim(@Param("delivery") FlowPendingDelivery delivery, @Param("claimId") String claimId,
              @Param("now") LocalDateTime now, @Param("until") LocalDateTime until);

    int reschedule(@Param("identity") FlowEventDeliveryIdentity identity, @Param("claimId") String claimId,
                   @Param("next") LocalDateTime next, @Param("error") String error);
}
