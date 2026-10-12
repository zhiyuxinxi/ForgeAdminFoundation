package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.dto.FlowEntrySubmitDTO;
import com.mdframe.forge.starter.flow.service.FlowRuntimeService;
import com.mdframe.forge.starter.flow.vo.FlowEntryRuntimeVO;
import com.mdframe.forge.starter.flow.vo.FlowFormInstanceVO;
import com.mdframe.forge.starter.flow.vo.FlowStartResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 流程入口运行态接口。
 */
@RestController
@RequestMapping("/api/flow/runtime")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowRuntimeController {

    private final FlowRuntimeService flowRuntimeService;

    @GetMapping("/entry/{entryCode}")
    public RespInfo<FlowEntryRuntimeVO> entry(@PathVariable String entryCode) {
        return RespInfo.success(flowRuntimeService.getRuntimeEntry(entryCode));
    }

    @PostMapping("/submit/{entryCode}")
    public RespInfo<FlowStartResultVO> submit(@PathVariable String entryCode,
                                              @RequestBody FlowEntrySubmitDTO dto) {
        return RespInfo.success("提交成功", flowRuntimeService.submitEntryForm(entryCode, dto));
    }

    @GetMapping("/instance/{processInstanceId}")
    public RespInfo<FlowFormInstanceVO> instance(@PathVariable String processInstanceId) {
        return RespInfo.success(flowRuntimeService.getInstanceByProcessInstanceId(processInstanceId));
    }
}
