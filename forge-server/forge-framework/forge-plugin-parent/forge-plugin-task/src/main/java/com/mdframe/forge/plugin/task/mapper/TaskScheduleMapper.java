package com.mdframe.forge.plugin.task.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.mdframe.forge.plugin.task.entity.TaskOccurrenceRow;
import com.mdframe.forge.plugin.task.entity.TaskScheduleRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TaskScheduleMapper {
    int insert(TaskScheduleRow row);
    TaskScheduleRow byKey(@Param("tenant") long tenant, @Param("code") String code, @Param("version") int version);
    TaskScheduleRow get(@Param("tenant") long tenant, @Param("id") long id, @Param("lock") boolean lock);
    List<TaskScheduleRow> list(@Param("tenant") long tenant, @Param("after") Long after, @Param("limit") int limit);
    int update(TaskScheduleRow row);
    /** Scheduler-only discovery of persisted principals; never called by an HTTP lookup. */
    @InterceptorIgnore(tenantLine = "true")
    List<TaskScheduleRow> due(@Param("now") LocalDateTime now);
    TaskOccurrenceRow occurrence(@Param("tenant") long tenant, @Param("schedule") long schedule,
                                 @Param("index") int index);
    int insertOccurrence(TaskOccurrenceRow row);
    int markGenerated(@Param("tenant") long tenant, @Param("id") long id);
    int link(@Param("tenant") long tenant, @Param("occurrence") long occurrence,
             @Param("task") long task, @Param("assignee") long assignee);
    TaskOccurrenceRow forTask(@Param("tenant") long tenant, @Param("task") long task);
}
