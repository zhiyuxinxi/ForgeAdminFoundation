package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.BusinessRuntimeContext;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.BindingLookupResult;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.*;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowDisplayText.*;

/** Coordinates one document flow start within the caller's transaction, including its lock lifetime. */
@Slf4j
@RequiredArgsConstructor
final class BusinessFlowStartCoordinator {
    private static final String FLOW_START_LOCK_PREFIX = "forge:business-flow:start:";
    private static final long FLOW_START_LOCK_WAIT_SECONDS = 5L;

    private final BusinessFlowRuntimeMetadata runtimeMetadata;
    private final BusinessFlowInstanceLinkMapper flowInstanceLinkMapper;
    private final BusinessDocumentRuntimeService documentRuntimeService;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessFlowStatusUpdates statusUpdates;
    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final BusinessFlowStartRecovery startRecovery;
    private final LocalFlowStartLocks localFlowStartLocks = new LocalFlowStartLocks();
    private final FlowBindingConfiguration bindingConfiguration = new FlowBindingConfiguration();

    BusinessFlowRuntimeVO start(StartRequest request, FlowClient flowClient) {
        BusinessFlowStartDTO dto = request.dto();
        boolean checkPermission = request.checkPermission();
        Long starterUserId = request.starterUserId();
        Long tenantId = request.tenantId();
        boolean stableBusinessKey = request.stableBusinessKey();
        boolean allowDraftRuntime = request.allowDraftRuntime();
        if (dto == null) {
            throw new BusinessException("发起主流程参数不能为空");
        }
        if (StringUtils.isBlank(dto.getObjectCode())) {
            throw new BusinessException("业务对象编码不能为空");
        }
        if (dto.getRecordId() == null) {
            throw new BusinessException("请先保存记录后再发起主流程");
        }
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法发起主流程");
        }
        if (dto.getPreviousProcessInstanceId() != null && StringUtils.isBlank(dto.getStartRequestId())) {
            throw new BusinessException(UiMessages.message("flow.start.request.required"));
        }

        FlowStartContext startContext = resolveFlowStartContext(tenantId, dto.getObjectCode(), allowDraftRuntime);
        String objectCode = startContext.objectCode();
        String configKey = startContext.configKey();
        Map<String, Object> recordData = allowDraftRuntime
                ? dynamicCrudService.selectByIdAllowDraft(configKey, dto.getRecordId())
                : dynamicCrudService.selectById(configKey, dto.getRecordId());
        if (recordData == null) {
            log.warn("[低代码流程启动] 业务记录查询为空: tenantId={}, objectCode={}, configKey={}, recordId={}, "
                            + "starterUserId={}, activeOrgId={}, checkPermission={}, stableBusinessKey={}",
                    tenantId, objectCode, configKey, dto.getRecordId(),
                    starterUserId != null ? starterUserId : resolveUserId(),
                    resolveActiveOrgId(), checkPermission, stableBusinessKey);
            throw new BusinessException(
                    404,
                    UiMessages.translate("记录不存在或无权限访问，请使用当前委托用户可见的已保存业务记录 ID"));
        }

        String businessKey = objectCode + ":" + dto.getRecordId();
        return executeWithFlowStartLock(tenantId, businessKey,
                () -> startDocumentFlowLocked(request, startContext, recordData, businessKey, flowClient));
    }

    private BusinessFlowRuntimeVO startDocumentFlowLocked(StartRequest request, FlowStartContext context,
                                                          Map<String, Object> recordData, String businessKey,
                                                          FlowClient flowClient) {
        BusinessFlowStartDTO dto = request.dto();
        boolean checkPermission = request.checkPermission();
        Long starterUserId = request.starterUserId();
        String starterUserName = request.starterUserName();
        Long tenantId = request.tenantId();
        boolean stableBusinessKey = request.stableBusinessKey();
        AiBusinessDocumentConfig documentConfig = context.documentConfig();
        AiCrudConfig runtimeConfig = context.runtimeConfig();
        String objectCode = context.objectCode();
        String configKey = context.configKey();
        String requestedObjectCode = context.requestedObjectCode();
        Long userId = starterUserId != null ? starterUserId : resolveUserId();
        BusinessFlowStartRequestIdentity identity = BusinessFlowStartRequestIdentity.from(
                tenantId, businessKey, userId, dto, stableBusinessKey);
        BusinessFlowRuntimeVO replay = findExplicitReplay(request, context, businessKey, identity, flowClient);
        if (replay != null) return replay;
        if (documentConfig != null) {
            documentRuntimeService.validateStartAllowed(objectCode, dto.getRecordId(), checkPermission);
        }
        AiBusinessFlowInstanceLink runningLink = flowInstanceLinkMapper.selectRunningByBusinessKey(tenantId, businessKey);
        if (runningLink != null && dto.getStartRequestId() == null) {
            return BusinessFlowStatusReader.toRuntimeVO(runningLink, "当前单据已有流转中的流程");
        }

        AiBusinessBinding binding = selectFlowBindingForStart(tenantId, objectCode, requestedObjectCode);
        JSONObject bindingConfig = binding == null ? new JSONObject() : readJsonObject(binding.getBindingConfig());
        runtimeMetadata.ensureBusinessBinding(bindingConfig, tenantId, objectCode);
        String dtoFlowModelKey = StringUtils.trimToNull(dto.getFlowModelKey());
        String bindingConfigFlowModelKey = bindingConfiguration.resolveFlowModelKey(bindingConfig);
        String bindingKey = binding == null ? null : StringUtils.trimToNull(binding.getBindingKey());
        String documentDefaultFlowKey = documentConfig == null ? null : StringUtils.trimToNull(documentConfig.getDefaultFlowKey());
        String flowModelKey = StringUtils.firstNonBlank(
                dtoFlowModelKey,
                bindingConfigFlowModelKey,
                bindingKey,
                documentDefaultFlowKey);
        if (StringUtils.isBlank(flowModelKey)) {
            log.warn("[低代码流程启动] 主流程解析失败: tenantId={}, objectCode={}, recordId={}, checkPermission={}, " +
                            "configKey={}, documentConfigId={}, documentEnabled={}, documentDefaultFlowKey={}, " +
                            "runtimeConfigId={}, runtimeConfigKey={}, binding={}, dtoFlowModelKey={}, " +
                            "bindingConfigFlowModelKey={}, bindingConfigPreview={}",
                    tenantId, objectCode, dto.getRecordId(), checkPermission, configKey,
                    documentConfig == null ? null : documentConfig.getId(),
                    documentConfig == null ? null : documentConfig.getDocumentEnabled(),
                    documentDefaultFlowKey,
                    runtimeConfig == null ? null : runtimeConfig.getId(),
                    runtimeConfig == null ? null : runtimeConfig.getConfigKey(),
                    describeBinding(binding), dtoFlowModelKey, bindingConfigFlowModelKey, previewBindingConfig(bindingConfig));
            throw new BusinessException("请先在流程与自动化中配置主流程");
        }
        log.info("[低代码流程启动] 主流程解析成功: tenantId={}, objectCode={}, recordId={}, configKey={}, " +
                        "flowModelKey={}, bindingId={}, bindingType={}",
                tenantId, objectCode, dto.getRecordId(), configKey, flowModelKey,
                binding == null ? null : binding.getId(), binding == null ? null : binding.getBindingType());

        Map<String, Object> flowVariables = buildFlowVariables(bindingConfig, recordData);
        if (dto.getVariables() != null) {
            flowVariables.putAll(dto.getVariables());
        }
        flowVariables.putIfAbsent("objectCode", objectCode);
        flowVariables.putIfAbsent("configKey", configKey);
        flowVariables.putIfAbsent("recordId", dto.getRecordId());
        flowVariables.putIfAbsent("businessKey", businessKey);
        String flowBusinessKey = stableBusinessKey || dto.getStartRequestId() != null
                ? businessKey : resolveFlowBusinessKeyForStart(tenantId, businessKey);
        flowVariables.putIfAbsent("documentBusinessKey", businessKey);
        flowVariables.putIfAbsent("recordBusinessKey", businessKey);
        flowVariables.put("flowBusinessKey", flowBusinessKey);

        String title = applyTitleTemplate(
                StringUtils.defaultIfBlank(dto.getTitle(), buildFlowTitle(bindingConfig, recordData, objectCode)),
                recordData,
                objectCode);
        String userName = StringUtils.defaultIfBlank(starterUserName, resolveUsername());
        FlowResult<String> result;
        if (dto.getStartRequestId() != null) {
            result = stableBusinessKey
                    ? flowClient.startProcessForDelegatedUser(flowModelKey, flowBusinessKey, objectCode,
                            title, flowVariables, dto.getStartRequestId(), dto.getPreviousProcessInstanceId())
                    : flowClient.startProcess(flowModelKey, flowBusinessKey, objectCode, title, flowVariables,
                            userId == null ? null : String.valueOf(userId), userName, null, null,
                            dto.getStartRequestId(), dto.getPreviousProcessInstanceId());
        } else {
            result = stableBusinessKey
                    ? flowClient.startProcessForDelegatedUser(
                            flowModelKey, flowBusinessKey, objectCode, title, flowVariables)
                    : flowClient.startProcess(
                            flowModelKey, flowBusinessKey, title, flowVariables,
                            userId == null ? null : String.valueOf(userId), userName, null, null);
        }
        if (result == null || !result.isSuccess() || StringUtils.isBlank(result.getData())) {
            throw new BusinessException(UiMessages.translate("流程发起失败: ") + (result == null ? "无返回结果" : result.getMsg()));
        }
        if (dto.getStartRequestId() != null) {
            AiBusinessFlowInstanceLink existing = flowInstanceLinkMapper.selectByProcessInstanceIdForUpdate(
                    tenantId, result.getData());
            if (existing != null) {
                if (!businessKey.equals(existing.getBusinessKey())) {
                    throw new BusinessException(UiMessages.message("flow.start.instance.mismatch"));
                }
                startRecovery.reconcileExisting(tenantId, businessKey, existing, dto, flowClient);
                return BusinessFlowStatusReader.toRuntimeVO(existing, "流程已发起");
            }
        }

        FlowEventMessage terminalEvent = dto.getStartRequestId() == null ? null
                : startRecovery.prepare(tenantId, businessKey, result.getData(), dto, flowClient);
        AiBusinessFlowInstanceLink link = new AiBusinessFlowInstanceLink();
        link.setTenantId(tenantId);
        link.setObjectCode(objectCode);
        link.setRecordId(dto.getRecordId());
        link.setBusinessKey(businessKey);
        link.setFlowModelKey(flowModelKey);
        link.setProcessInstanceId(result.getData());
        link.setFlowStatus("RUNNING");
        link.setStartUserId(userId);
        if (identity != null) identity.bind(link);
        link.setStartTime(LocalDateTime.now());
        link.setVariablesSnapshot(JSON.toJSONString(flowVariables));
        flowInstanceLinkMapper.insert(link);

        AiCrudConfig statusRuntimeConfig = runtimeConfig != null
                ? runtimeConfig : runtimeMetadata.resolvePublishedRuntimeConfig(tenantId, objectCode);
        if (StringUtils.isBlank(statusUpdates.managedField(dto.getVariables()))) {
            statusUpdates.update(documentConfig, runtimeConfig, bindingConfig, dto.getRecordId(), "IN_PROCESS");
        }
        statusUpdates.syncManaged(statusRuntimeConfig, dto.getRecordId(), dto.getVariables(), "IN_PROCESS");
        startRecovery.apply(link, terminalEvent);
        return BusinessFlowStatusReader.toRuntimeVO(link, "流程已发起");
    }

    private BusinessFlowRuntimeVO findExplicitReplay(StartRequest request, FlowStartContext context,
                                                     String businessKey, BusinessFlowStartRequestIdentity identity,
                                                     FlowClient flowClient) {
        if (identity == null) return null;
        AiBusinessFlowInstanceLink existing = flowInstanceLinkMapper.selectByStartRequestForUpdate(
                request.tenantId(), businessKey, identity.requestId());
        if (existing == null) return null;
        identity.requireMatch(existing);
        if (context.documentConfig() != null) {
            documentRuntimeService.validateStartReplayAllowed(context.objectCode(), request.dto().getRecordId(),
                    request.checkPermission());
        }
        startRecovery.reconcileExisting(request.tenantId(), businessKey, existing, request.dto(), flowClient);
        return BusinessFlowStatusReader.toRuntimeVO(existing, "流程已发起");
    }

    private BusinessFlowRuntimeVO executeWithFlowStartLock(Long tenantId,
                                                           String businessKey,
                                                           Supplier<BusinessFlowRuntimeVO> supplier) {
        String lockKey = buildFlowStartLockKey(tenantId, businessKey);
        FlowStartLockHandle lockHandle = acquireFlowStartLock(lockKey);
        boolean unlockInFinally = true;
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                unlockInFinally = false;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        unlockFlowStartLock(lockHandle);
                    }
                });
            }
            return supplier.get();
        } finally {
            if (unlockInFinally) {
                unlockFlowStartLock(lockHandle);
            }
        }
    }

    private FlowStartLockHandle acquireFlowStartLock(String lockKey) {
        RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
        if (redissonClient != null) {
            RLock lock = redissonClient.getLock(lockKey);
            try {
                if (!lock.tryLock(FLOW_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                    throw new BusinessException("流程正在发起，请勿重复提交");
                }
                return new FlowStartLockHandle(lockKey, lock, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("流程发起锁等待被中断，请稍后重试");
            }
        }

        try {
            LocalFlowStartLocks.Handle localLock = localFlowStartLocks.tryAcquire(
                    lockKey, FLOW_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (localLock == null) {
                throw new BusinessException("流程正在发起，请勿重复提交");
            }
            return new FlowStartLockHandle(lockKey, null, localLock);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("流程发起锁等待被中断，请稍后重试");
        }
    }

    private void unlockFlowStartLock(FlowStartLockHandle lockHandle) {
        if (lockHandle == null) {
            return;
        }
        RLock redissonLock = lockHandle.redissonLock();
        if (redissonLock != null) {
            try {
                if (redissonLock.isHeldByCurrentThread()) {
                    redissonLock.unlock();
                }
            } catch (Exception e) {
                log.warn("[低代码流程启动] 释放流程发起分布式锁失败: lockKey={}, error={}",
                        lockHandle.lockKey(), e.getMessage());
            }
            return;
        }

        LocalFlowStartLocks.Handle localLock = lockHandle.localLock();
        if (localLock != null) {
            localLock.close();
        }
    }

    private String buildFlowStartLockKey(Long tenantId, String businessKey) {
        return FLOW_START_LOCK_PREFIX
                + safeLockToken(tenantId) + ":"
                + safeLockToken(businessKey);
    }

    private String safeLockToken(Object value) {
        if (value == null) {
            return "null";
        }
        return String.valueOf(value).replaceAll("[^A-Za-z0-9:_-]", "_");
    }

    private String resolveFlowBusinessKeyForStart(Long tenantId, String businessKey) {
        AiBusinessFlowInstanceLink latest = flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, businessKey);
        if (latest == null) {
            return businessKey;
        }
        return businessKey + ":R" + (latest.getId() == null ? System.currentTimeMillis() : latest.getId() + 1);
    }

    private String resolveStartConfigKey(AiBusinessDocumentConfig documentConfig,
                                         AiCrudConfig runtimeConfig,
                                         String fallbackConfigKey,
                                         boolean allowDraftRuntime) {
        if (documentConfig != null) {
            if (StringUtils.isNotBlank(documentConfig.getConfigKey())) {
                return documentConfig.getConfigKey();
            }
            if (allowDraftRuntime && StringUtils.isNotBlank(fallbackConfigKey)) {
                return fallbackConfigKey;
            }
            throw new BusinessException("单据缺少发布配置，无法发起主流程");
        }
        if (runtimeConfig != null && StringUtils.isNotBlank(runtimeConfig.getConfigKey())) {
            return runtimeConfig.getConfigKey();
        }
        if (allowDraftRuntime && StringUtils.isNotBlank(fallbackConfigKey)) {
            return fallbackConfigKey;
        }
        throw new BusinessException("业务对象缺少已发布运行配置，无法发起主流程");
    }

    private AiBusinessBinding selectFlowBindingForStart(Long tenantId, String objectCode, String... fallbackCodes) {
        BindingLookupResult result = runtimeMetadata.selectMainFlowBinding(tenantId, objectCode, fallbackCodes);
        if (isBindingEnabled(result.binding())) {
            if ("APPROVAL".equalsIgnoreCase(result.binding().getBindingType())) {
                log.info("[低代码流程启动] 使用历史审批绑定作为主流程: tenantId={}, objectCode={}, binding={}",
                        tenantId, result.matchedObjectCode(), describeBinding(result.binding()));
            }
            return result.binding();
        }
        if (result.binding() != null) {
            log.warn("[低代码流程启动] 未找到启用的主流程绑定: tenantId={}, objectCodes={}, binding={}",
                    tenantId, result.candidates(), describeBinding(result.binding()));
        } else {
            log.warn("[低代码流程启动] 未找到主流程绑定记录: tenantId={}, objectCodes={}", tenantId, result.candidates());
        }
        return null;
    }

    private String describeBinding(AiBusinessBinding binding) {
        if (binding == null) {
            return "null";
        }
        return "id=" + binding.getId()
                + ", type=" + binding.getBindingType()
                + ", status=" + binding.getStatus()
                + ", targetCode=" + binding.getTargetCode()
                + ", bindingKey=" + binding.getBindingKey()
                + ", bindingName=" + binding.getBindingName()
                + ", configBlank=" + StringUtils.isBlank(binding.getBindingConfig());
    }

    private String previewBindingConfig(JSONObject bindingConfig) {
        if (bindingConfig == null || bindingConfig.isEmpty()) {
            return "{}";
        }
        return StringUtils.left(bindingConfig.toJSONString(), 400);
    }

    private FlowStartContext resolveFlowStartContext(Long tenantId, String objectCodeOrConfigKey, boolean allowDraftRuntime) {
        BusinessRuntimeContext context = runtimeMetadata.resolveBusinessRuntimeContext(tenantId, objectCodeOrConfigKey, allowDraftRuntime);
        String configKey = resolveStartConfigKey(
                context.documentConfig(), context.runtimeConfig(), context.configKey(), allowDraftRuntime);
        return new FlowStartContext(
                context.requestedObjectCode(),
                context.objectCode(),
                configKey,
                context.documentConfig(),
                context.runtimeConfig());
    }

    static Map<String, Object> buildFlowVariables(JSONObject bindingConfig, Map<String, Object> recordData) {
        Map<String, Object> variables = new HashMap<>();
        JSONArray variableMapping = bindingConfig.getJSONArray("variableMapping");

        if (recordData != null) {
            for (Map.Entry<String, Object> entry : recordData.entrySet()) {
                putBusinessFieldVariable(variables, entry.getKey(), entry.getValue());
            }
        }

        if (variableMapping != null && recordData != null) {
            for (int i = 0; i < variableMapping.size(); i++) {
                JSONObject mapping = variableMapping.getJSONObject(i);
                String formField = StringUtils.defaultIfBlank(mapping.getString("formField"), mapping.getString("field"));
                String flowVariable = StringUtils.defaultIfBlank(mapping.getString("flowVariable"), mapping.getString("variable"));
                Object value = readRecordValue(recordData, formField);
                if (value != null && StringUtils.isNotBlank(flowVariable)) {
                    variables.put(flowVariable, value);
                }
            }
        }

        return variables;
    }

    static void putBusinessFieldVariable(Map<String, Object> variables, String field, Object value) {
        String key = StringUtils.trimToNull(field);
        if (key == null || value == null || value instanceof Map<?, ?> || value instanceof Iterable<?>) {
            return;
        }
        variables.putIfAbsent(key, value);
        variables.putIfAbsent(snakeToCamel(key), value);
        variables.putIfAbsent(camelToSnake(key), value);
    }

    static String buildFlowTitle(JSONObject bindingConfig, Map<String, Object> recordData, String objectCode) {
        String titleTemplate = bindingConfig.getString("titleTemplate");
        return applyTitleTemplate(titleTemplate, recordData, objectCode);
    }

    static String applyTitleTemplate(String titleTemplate, Map<String, Object> recordData, String objectCode) {
        if (StringUtils.isBlank(titleTemplate)) {
            return StringUtils.defaultIfBlank(objectCode, "业务") + " 审批申请";
        }
        String resolved = titleTemplate;
        if (recordData != null) {
            for (Map.Entry<String, Object> entry : recordData.entrySet()) {
                resolved = replaceTemplateValue(resolved, entry.getKey(), entry.getValue());
            }
        }
        return StringUtils.defaultIfBlank(StringUtils.trimToNull(resolved), objectCode + " 审批申请");
    }

    record StartRequest(BusinessFlowStartDTO dto, boolean checkPermission,
                        Long starterUserId, String starterUserName, Long tenantId,
                        boolean stableBusinessKey, boolean allowDraftRuntime) {
    }

    private record FlowStartContext(String requestedObjectCode,
                                    String objectCode,
                                    String configKey,
                                    AiBusinessDocumentConfig documentConfig,
                                    AiCrudConfig runtimeConfig) {
    }

    private record FlowStartLockHandle(String lockKey,
                                       RLock redissonLock,
                                       LocalFlowStartLocks.Handle localLock) {
    }

}
