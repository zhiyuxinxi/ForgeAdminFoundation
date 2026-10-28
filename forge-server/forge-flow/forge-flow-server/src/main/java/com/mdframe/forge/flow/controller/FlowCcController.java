package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowCc;
import com.mdframe.forge.starter.flow.service.FlowCcService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程抄送管理接口。
 */
@RestController
@RequestMapping("/api/flow/cc")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class FlowCcController {

    private final FlowCcService flowCcService;
    private final FlowTaskService flowTaskService;

    /**
     * 发送抄送。
     */
    @SaCheckPermission("flow:cc:send")
    @OperationLog(module = "流程抄送", type = OperationType.ADD, desc = "发送流程抄送")
    @PostMapping("/send")
    public RespInfo<Void> sendCc(@RequestBody Map<String, Object> params) {
        String processInstanceId = (String) params.get("processInstanceId");
        String processDefKey = (String) params.get("processDefKey");
        String taskId = (String) params.get("taskId");
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        String businessKey = (String) params.get("businessKey");
        @SuppressWarnings("unchecked")
        List<String> ccUserIds = (List<String>) params.get("ccUserIds");
        @SuppressWarnings("unchecked")
        List<String> ccUserNames = (List<String>) params.get("ccUserNames");
        String sendUserId = currentUserId();
        String sendUserName = SessionHelper.getUsername();

        flowCcService.sendCc(processInstanceId, processDefKey, taskId, title, content,
                businessKey, ccUserIds, ccUserNames, sendUserId, sendUserName);

        return RespInfo.success("抄送成功", null);
    }

    /**
     * 我的抄送（抄送给我的）。收件人身份只取当前登录会话。
     */
    @SaCheckPermission("flow:cc:my")
    @GetMapping("/my")
    public RespInfo<IPage<FlowCc>> myCc(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer isRead) {
        Page<FlowCc> page = new Page<>(pageNum, pageSize);
        IPage<FlowCc> result = flowCcService.myCc(page, currentUserId(), isRead);
        return RespInfo.success(result);
    }

    /**
     * 我发送的抄送。发送人身份只取当前登录会话。
     */
    @SaCheckPermission("flow:cc:sent")
    @GetMapping("/sent")
    public RespInfo<IPage<FlowCc>> sentCc(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<FlowCc> page = new Page<>(pageNum, pageSize);
        IPage<FlowCc> result = flowCcService.sentCc(page, currentUserId());
        return RespInfo.success(result);
    }

    /**
     * 获取当前收件人的抄送关联业务表单信息。
     */
    @SaCheckPermission("flow:cc:form")
    @GetMapping("/form/{id}")
    public RespInfo<TaskFormInfo> getCcFormInfo(@PathVariable String id) {
        FlowCc cc = getCurrentRecipientCc(id);
        if (cc == null) {
            return RespInfo.error(UiMessages.translate("抄送记录不存在"));
        }
        TaskFormInfo formInfo = flowTaskService.getProcessFormInfo(
                cc.getProcessInstanceId(),
                cc.getBusinessKey(),
                cc.getProcessDefKey(),
                cc.getTaskId(),
                null);
        return RespInfo.success(formInfo);
    }

    /**
     * 标记当前收件人的抄送为已读。
     */
    @SaCheckPermission("flow:cc:read")
    @OperationLog(module = "流程抄送", type = OperationType.UPDATE, desc = "标记本人抄送已读")
    @PostMapping("/read/{id}")
    public RespInfo<Void> markRead(@PathVariable String id) {
        if (getCurrentRecipientCc(id) == null) {
            return RespInfo.error(UiMessages.translate("抄送记录不存在"));
        }
        flowCcService.markRead(id);
        return RespInfo.success("已标记已读", null);
    }

    /**
     * 批量标记当前收件人的抄送为已读。
     */
    @SaCheckPermission("flow:cc:read")
    @OperationLog(module = "流程抄送", type = OperationType.UPDATE, desc = "批量标记本人抄送已读")
    @PostMapping("/read/batch")
    public RespInfo<Void> batchMarkRead(@RequestBody List<String> ids) {
        Set<String> requestedIds = ids == null ? Set.of() : new HashSet<>(ids);
        if (!requestedIds.isEmpty()) {
            String userId = currentUserId();
            List<FlowCc> records = flowCcService.listByIds(requestedIds);
            boolean ownsAll = records.size() == requestedIds.size()
                    && records.stream().allMatch(cc -> userId.equals(cc.getCcUserId()));
            if (!ownsAll) {
                return RespInfo.error(UiMessages.translate("抄送记录不存在"));
            }
        }
        flowCcService.batchMarkRead(List.copyOf(requestedIds));
        return RespInfo.success("已批量标记已读", null);
    }

    /**
     * 获取当前收件人的未读抄送数量。
     */
    @SaCheckPermission("flow:cc:my")
    @GetMapping("/unread/count")
    public RespInfo<Map<String, Object>> countUnread() {
        long count = flowCcService.countUnread(currentUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return RespInfo.success(result);
    }

    private FlowCc getCurrentRecipientCc(String id) {
        FlowCc cc = flowCcService.getById(id);
        return cc != null && currentUserId().equals(cc.getCcUserId()) ? cc : null;
    }

    private String currentUserId() {
        Long userId = SessionHelper.getUserId();
        if (userId == null) {
            throw new IllegalStateException(UiMessages.translate("缺少登录用户上下文"));
        }
        return String.valueOf(userId);
    }
}
