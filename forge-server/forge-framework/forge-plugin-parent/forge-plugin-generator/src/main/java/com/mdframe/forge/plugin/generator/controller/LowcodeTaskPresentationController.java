package com.mdframe.forge.plugin.generator.controller;

import com.mdframe.forge.plugin.generator.service.task.LowcodeTaskPresentation;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only presentation; lifecycle mutations remain owned by BusinessTaskEngine. */
@RestController
@RequiredArgsConstructor
public class LowcodeTaskPresentationController {
    private final LowcodeTaskPresentation presentation;

    @GetMapping("/ai/business-tasks/{taskId}/form")
    public RespInfo<LowcodeTaskPresentation.Form> form(@PathVariable long taskId) {
        return RespInfo.success(presentation.form(taskId));
    }

    @GetMapping("/ai/crud/{configKey}/task-actions")
    public RespInfo<List<LowcodeTaskPresentation.Action>> actions(@PathVariable String configKey) {
        return RespInfo.success(presentation.actions(configKey));
    }
}
