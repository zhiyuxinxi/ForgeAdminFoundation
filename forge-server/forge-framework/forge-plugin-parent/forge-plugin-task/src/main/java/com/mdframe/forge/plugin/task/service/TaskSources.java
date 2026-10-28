package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.task.api.TaskSourceHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One installed owner per source type; no reflection or client-selected execution target. */
@Component
public class TaskSources {
    private final Map<String, TaskSourceHandler> handlers;

    public TaskSources(List<TaskSourceHandler> sources) {
        Map<String, TaskSourceHandler> registered = new HashMap<>();
        for (TaskSourceHandler source : sources) {
            String key = source.sourceType();
            if (!"master".equals(source.transactionDataSource())) {
                throw new IllegalStateException("Task source write-back requires the master datasource");
            }
            if (key == null || !key.matches("[a-z][a-z0-9_.-]{0,63}")
                    || registered.putIfAbsent(key, source) != null) {
                throw new IllegalStateException("Invalid or duplicate task source owner");
            }
        }
        handlers = Map.copyOf(registered);
    }

    TaskSourceHandler require(String sourceType) {
        TaskSourceHandler handler = handlers.get(sourceType);
        if (handler == null) {
            throw TaskMessages.error(409, "source.unavailable");
        }
        return handler;
    }
}
