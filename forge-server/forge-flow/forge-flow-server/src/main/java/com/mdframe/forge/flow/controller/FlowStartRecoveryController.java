package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import com.mdframe.forge.starter.core.domain.FlowStartRecoverySnapshot;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.flow.service.support.FlowStartRecoveryQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flow/instance")
@ApiEncrypt
@IgnoreTenant
public class FlowStartRecoveryController {
    private final FlowStartRecoveryQuery query;

    public FlowStartRecoveryController(FlowStartRecoveryQuery query) {
        this.query = query;
    }

    @SaCheckLogin
    @GetMapping(value = "/status/{businessKey}", params = "startRequestId")
    public RespInfo<FlowStartOutcome> getStartOutcome(@PathVariable("businessKey") String businessKey,
                                                     @RequestParam("startRequestId") String startRequestId) {
        LoginUser user = currentUser();
        return RespInfo.success(query.find(user.getTenantId(), user.getUserId(), businessKey, startRequestId));
    }

    @SaCheckLogin
    @GetMapping("/recovery/{businessKey}")
    public RespInfo<FlowStartRecoverySnapshot> getRecovery(@PathVariable("businessKey") String businessKey,
                                                         @RequestParam("startRequestId") String startRequestId) {
        LoginUser user = currentUser();
        return RespInfo.success(query.findRecovery(user.getTenantId(), user.getUserId(), businessKey, startRequestId));
    }

    private LoginUser currentUser() {
        StpUtil.checkLogin();
        Object value = StpUtil.getTokenSession().get("loginUser");
        if (!(value instanceof LoginUser user)) {
            throw new BusinessException(401, "FLOW_START_QUERY_SESSION_REQUIRED");
        }
        return user;
    }
}
