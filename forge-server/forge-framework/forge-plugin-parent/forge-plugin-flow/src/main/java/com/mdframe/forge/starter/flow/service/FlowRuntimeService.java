package com.mdframe.forge.starter.flow.service;

import com.mdframe.forge.starter.flow.dto.FlowEntrySubmitDTO;
import com.mdframe.forge.starter.flow.vo.FlowEntryRuntimeVO;
import com.mdframe.forge.starter.flow.vo.FlowFormInstanceVO;
import com.mdframe.forge.starter.flow.vo.FlowStartResultVO;

/**
 * 流程入口运行态服务。
 */
public interface FlowRuntimeService {

    FlowEntryRuntimeVO getRuntimeEntry(String entryCode);

    FlowStartResultVO submitEntryForm(String entryCode, FlowEntrySubmitDTO dto);

    FlowFormInstanceVO getInstanceByProcessInstanceId(String processInstanceId);
}
