package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.flow.event.FlowEventAcknowledgementService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated service ACK; no identity or tenant is accepted from the request body. */
@RestController
@RequestMapping("/api/flow/events")
@IgnoreTenant
public class FlowEventAcknowledgementController {
    private final FlowEventAcknowledgementService service;

    public FlowEventAcknowledgementController(FlowEventAcknowledgementService service) {
        this.service = service;
    }

    @PostMapping("/ack")
    @SaCheckPermission("flow:event:ack")
    public RespInfo<String> acknowledge(@RequestBody Acknowledgement request) {
        StpUtil.checkLogin();
        StpUtil.checkPermission("flow:event:ack");
        Object value = StpUtil.getTokenSession().get("loginUser");
        if (!(value instanceof LoginUser user) || user.getTenantId() == null || user.getTenantId() <= 0) {
            throw new IllegalArgumentException("FLOW_EVENT_ACK_SESSION_REQUIRED");
        }
        return RespInfo.success(service.acknowledge(request.eventId(), StpUtil.getLoginIdAsString(), user.getTenantId()));
    }

    public record Acknowledgement(String eventId) {
    }
}
