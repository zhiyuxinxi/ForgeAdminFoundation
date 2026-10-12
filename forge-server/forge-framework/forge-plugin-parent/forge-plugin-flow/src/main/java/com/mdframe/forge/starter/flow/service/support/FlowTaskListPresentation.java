package com.mdframe.forge.starter.flow.service.support;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayAdapter;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Applies user and business presentation after task selection has completed. */
@Component
@Slf4j
public class FlowTaskListPresentation {
    private final FlowUserDisplayNames userDisplayNames;
    private final FlowBusinessListDisplayAdapter flowBusinessListDisplayAdapter;

    public FlowTaskListPresentation(FlowUserDisplayNames userDisplayNames,
                                    @Nullable FlowBusinessListDisplayAdapter adapter) {
        this.userDisplayNames = userDisplayNames;
        this.flowBusinessListDisplayAdapter = adapter;
    }

    public IPage<FlowTask> enrich(IPage<FlowTask> page) {
        enrichTaskUserNames(page);
        if (flowBusinessListDisplayAdapter == null || page == null || page.getRecords() == null
                || page.getRecords().isEmpty()) {
            return page;
        }
        List<FlowBusinessListDisplayItem> items = page.getRecords().stream()
                .map(this::toDisplayItem)
                .collect(Collectors.toList());
        try {
            flowBusinessListDisplayAdapter.enrich(items);
            for (int i = 0; i < page.getRecords().size(); i++) {
                applyDisplayItem(page.getRecords().get(i), items.get(i));
            }
        } catch (Exception e) {
            log.warn("补齐流程任务业务摘要失败，继续返回流程基础信息: {}", e.getMessage());
        }
        return page;
    }

    /**
     * 任务表历史上同时保存过账号和姓名，且低代码流程曾把 username 写入 start_user_name。
     * 列表返回前统一按稳定的用户 ID 反查真实姓名，保证待办、已办和我发起的展示一致。
     */
    private void enrichTaskUserNames(IPage<FlowTask> page) {
        if (page == null || page.getRecords() == null || page.getRecords().isEmpty()) {
            return;
        }
        for (FlowTask task : page.getRecords()) {
            if (task == null) {
                continue;
            }
            task.setStartUserName(userDisplayNames.resolve(task.getStartUserId(), task.getStartUserName()));

            String assigneeName = userDisplayNames.resolve(task.getAssignee(), task.getAssigneeName());
            if (isBlank(assigneeName) && !isBlank(task.getCandidateUsers())) {
                assigneeName = resolveUserNames(task.getCandidateUsers());
            }
            task.setAssigneeName(assigneeName);
        }
    }

    private String resolveUserNames(String userIds) {
        return Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> userDisplayNames.resolve(value, value))
                .filter(value -> !isBlank(value))
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private FlowBusinessListDisplayItem toDisplayItem(FlowTask task) {
        FlowBusinessListDisplayItem item = new FlowBusinessListDisplayItem();
        item.setBusinessKey(task.getBusinessKey());
        item.setProcessInstanceId(task.getProcessInstanceId());
        item.setProcessDefKey(task.getProcessDefKey());
        item.setProcessName(task.getProcessName());
        item.setProcessDefinitionName(task.getProcessDefinitionName());
        item.setTaskId(task.getTaskId());
        item.setTaskName(task.getTaskName());
        item.setTitle(task.getTitle());
        item.setObjectCode(task.getObjectCode());
        item.setRecordId(task.getRecordId());
        item.setBusinessObjectName(task.getBusinessObjectName());
        item.setBusinessSummary(task.getBusinessSummary());
        return item;
    }

    private void applyDisplayItem(FlowTask task, FlowBusinessListDisplayItem item) {
        if (item == null) {
            return;
        }
        task.setObjectCode(firstNonBlank(item.getObjectCode(), task.getObjectCode()));
        task.setRecordId(item.getRecordId() != null ? item.getRecordId() : task.getRecordId());
        task.setBusinessObjectName(firstNonBlank(item.getBusinessObjectName(), task.getBusinessObjectName()));
        task.setBusinessSummary(firstNonBlank(item.getBusinessSummary(), task.getBusinessSummary()));
        task.setProcessName(firstNonBlank(task.getProcessName(), item.getProcessName()));
        task.setProcessDefinitionName(firstNonBlank(
                task.getProcessDefinitionName(),
                item.getProcessDefinitionName(),
                task.getProcessName(),
                task.getProcessDefKey()));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
