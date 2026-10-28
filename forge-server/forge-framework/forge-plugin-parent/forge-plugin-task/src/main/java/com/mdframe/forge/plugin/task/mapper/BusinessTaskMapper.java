package com.mdframe.forge.plugin.task.mapper;

import com.mdframe.forge.plugin.task.entity.BusinessTaskRow;
import com.mdframe.forge.plugin.task.entity.TaskDefinitionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Internal task-owner queries. Every statement has explicit trusted tenant predicates. */
@Mapper
public interface BusinessTaskMapper {
    int insertDefinition(TaskDefinitionRow row);
    TaskDefinitionRow definition(@Param("tenantId") long tenantId, @Param("id") long id);
    TaskDefinitionRow definitionKey(@Param("tenantId") long tenantId, @Param("sourceType") String sourceType,
                                    @Param("code") String code, @Param("definitionVersion") int version);
    List<TaskDefinitionRow> definitions(@Param("tenantId") long tenantId, @Param("sourceType") String sourceType,
                                       @Param("afterId") Long afterId, @Param("limit") int limit);
    int insertTask(BusinessTaskRow row);
    BusinessTaskRow taskKey(BusinessTaskRow key);
    BusinessTaskRow task(@Param("tenantId") long tenantId, @Param("id") long id, @Param("lock") boolean lock);
    List<BusinessTaskRow> assigned(@Param("tenantId") long tenantId, @Param("assigneeId") long assigneeId,
                                   @Param("state") String state, @Param("afterId") Long afterId, @Param("limit") int limit);
    int transition(BusinessTaskRow row);
}
