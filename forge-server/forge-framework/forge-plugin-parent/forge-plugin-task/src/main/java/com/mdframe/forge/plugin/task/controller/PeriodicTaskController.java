package com.mdframe.forge.plugin.task.controller;

import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.task.api.PeriodicTaskEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/task/schedules")
@RequiredArgsConstructor
public class PeriodicTaskController {
    private final PeriodicTaskEngine engine;

    @PostMapping
    public RespInfo<PeriodicTaskEngine.Schedule> define(@RequestBody PeriodicTaskEngine.Define command) {
        return RespInfo.success(engine.define(command));
    }

    @GetMapping("/{id}")
    public RespInfo<PeriodicTaskEngine.Schedule> get(@PathVariable long id) {
        return RespInfo.success(engine.get(id));
    }

    @GetMapping
    public RespInfo<List<PeriodicTaskEngine.Schedule>> list(@RequestParam(required = false) Long afterId,
                                                           @RequestParam(defaultValue = "50") int limit) {
        return RespInfo.success(engine.list(afterId, limit));
    }

    @PostMapping("/preview")
    public RespInfo<List<PeriodicTaskEngine.Occurrence>> preview(@RequestBody Preview command) {
        return RespInfo.success(engine.preview(command.calendar(), command.fromIndex(), command.count()));
    }

    @PostMapping("/{id}/enabled")
    public RespInfo<PeriodicTaskEngine.Schedule> enable(@PathVariable long id, @RequestBody Enabled command) {
        return RespInfo.success(engine.enable(id, command.enabled()));
    }

    @PostMapping("/{id}/generate")
    public RespInfo<PeriodicTaskEngine.Run> generate(@PathVariable long id,
                                                   @RequestParam(defaultValue = "10") int limit) {
        return RespInfo.success(engine.generate(id, limit));
    }

    public record Preview(PeriodicTaskEngine.Calendar calendar, int fromIndex, int count) { }
    public record Enabled(boolean enabled) { }
}
