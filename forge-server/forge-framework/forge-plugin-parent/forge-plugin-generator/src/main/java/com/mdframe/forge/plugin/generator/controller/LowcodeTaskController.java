package com.mdframe.forge.plugin.generator.controller;

import com.mdframe.forge.plugin.generator.service.task.LowcodeTaskService;
import com.mdframe.forge.plugin.generator.service.task.PublishedTaskBinding;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.task.api.BusinessTask;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/crud/{configKey}/tasks")
@RequiredArgsConstructor
public class LowcodeTaskController {
    private final LowcodeTaskService tasks;

    @GetMapping
    public RespInfo<List<PublishedTaskBinding>> bindings(@PathVariable String configKey) {
        return RespInfo.success(tasks.bindings(configKey));
    }

    @PostMapping("/{bindingKey}/{recordId}")
    public RespInfo<BusinessTask> create(@PathVariable String configKey, @PathVariable String bindingKey,
                                         @PathVariable String recordId) {
        return RespInfo.success(tasks.create(configKey, bindingKey, recordId));
    }

    @GetMapping("/{bindingKey}/{recordId}")
    public RespInfo<BusinessTask> linked(@PathVariable String configKey, @PathVariable String bindingKey,
                                         @PathVariable String recordId) {
        return RespInfo.success(tasks.linked(configKey, bindingKey, recordId));
    }
}
