package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowResubmitDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskActionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowBindingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.BusinessRuntimeContext;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.BindingLookupResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务流程服务。
 * <p>
 * 负责业务对象与流程引擎的动态集成：
 * - 读取 ai_business_binding (binding_type=FLOW) 中的流程绑定配置
 * - 从业务记录动态发起流程（不依赖硬编码注解）
 * - 处理流程回调，更新业务记录状态
 */
@Slf4j
@Service
@FlowBind(modelKey = "*", businessType = "lowcode-business")
public class BusinessFlowService {

    @Autowired(required = false)
    private FlowClient flowClient;

    /**
     * 应用页面表单资产属于应用设计快照，不属于业务对象本身。
     * 使用字段注入保持已有测试/插件扩展的构造器兼容性。
     */
    @Autowired(required = false)
    private BusinessApplicationService businessApplicationService;

    private final BusinessBindingMapper bindingMapper;
    private final BusinessFlowInstanceLinkMapper flowInstanceLinkMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessDocumentRuntimeService documentRuntimeService;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessFieldDesignService businessFieldDesignService;
    private final BusinessFlowVariableResolver variableResolver;
    private final BusinessCodeFormProviderRegistry codeFormProviderRegistry;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final ObjectProvider<BusinessActionExecutionService> actionExecutionServiceProvider;
    private final BusinessFlowStartCoordinator flowStarts;
    private final FlowTaskAuthorization taskAuthorization = new FlowTaskAuthorization();
    private final TaskFormProjection taskFormProjection = new TaskFormProjection();
    private final BusinessCodeTaskFields codeTaskFields = new BusinessCodeTaskFields(taskFormProjection);
    private final NodeFormConfiguration nodeFormConfiguration = new NodeFormConfiguration();
    private final FlowBindingConfiguration bindingConfiguration = new FlowBindingConfiguration();
    private final FlowBindingPresentation bindingPresentation = new FlowBindingPresentation(bindingConfiguration);

    public BusinessFlowService(BusinessBindingMapper bindingMapper,
                               BusinessFlowInstanceLinkMapper flowInstanceLinkMapper,
                               AiCrudConfigMapper crudConfigMapper,
                               BusinessObjectMapper businessObjectMapper,
                               BusinessDocumentConfigService documentConfigService,
                               BusinessDocumentRuntimeService documentRuntimeService,
                               DynamicCrudService dynamicCrudService,
                               BusinessFieldDesignService businessFieldDesignService,
                               BusinessFlowVariableResolver variableResolver,
                               BusinessCodeFormProviderRegistry codeFormProviderRegistry,
                               ApplicationEventPublisher applicationEventPublisher,
                               ObjectProvider<RedissonClient> redissonClientProvider,
                               ObjectProvider<BusinessActionExecutionService> actionExecutionServiceProvider) {
        this.bindingMapper = bindingMapper;
        this.flowInstanceLinkMapper = flowInstanceLinkMapper;
        this.crudConfigMapper = crudConfigMapper;
        this.businessObjectMapper = businessObjectMapper;
        this.documentConfigService = documentConfigService;
        this.documentRuntimeService = documentRuntimeService;
        this.dynamicCrudService = dynamicCrudService;
        this.businessFieldDesignService = businessFieldDesignService;
        this.variableResolver = variableResolver;
        this.codeFormProviderRegistry = codeFormProviderRegistry;
        this.applicationEventPublisher = applicationEventPublisher;
        this.redissonClientProvider = redissonClientProvider;
        this.actionExecutionServiceProvider = actionExecutionServiceProvider;
        this.flowStarts = new BusinessFlowStartCoordinator(runtimeMetadata(), flowInstanceLinkMapper,
                documentRuntimeService, dynamicCrudService, statusUpdates(), redissonClientProvider,
                new BusinessFlowStartRecovery(flowInstanceLinkMapper, callbackResults()));
    }

    // Stateless read collaborators keep the existing extension constructor and optional FlowClient contract.
    private AiBusinessDocumentConfig resolveEnabledDocumentConfig(Long tenantId, String objectCodeOrConfigKey,
                                                                  AiCrudConfig runtimeConfig) {
        return runtimeMetadata().resolveEnabledDocumentConfig(tenantId, objectCodeOrConfigKey, runtimeConfig);
    }

    private BusinessFlowRuntimeMetadata runtimeMetadata() {
        return new BusinessFlowRuntimeMetadata(bindingMapper, crudConfigMapper, businessObjectMapper, documentConfigService);
    }

    private BusinessFlowListViews listViews() {
        return new BusinessFlowListViews(runtimeMetadata(), flowInstanceLinkMapper, dynamicCrudService, codeFormProviderRegistry);
    }

    private BusinessFlowStatusUpdates statusUpdates() {
        return new BusinessFlowStatusUpdates(documentConfigService, dynamicCrudService);
    }

    private BusinessFlowStatusReader statusReader() {
        return new BusinessFlowStatusReader(runtimeMetadata(), flowInstanceLinkMapper, flowClient);
    }

    private void appendUniqueFormAssets(List<Map<String, Object>> target, List<Map<String, Object>> source) {
        FormAssetMerger.appendUniqueFormAssets(target, source);
    }

    private void appendObjectFieldRegistryFallback(List<Map<String, Object>> assets, BusinessObjectVO object) {
        formAssetCatalog().runtime.appendObjectFieldRegistryFallback(assets, object);
    }

    private JSONObject buildObjectFieldRegistryFormSchema(BusinessObjectVO object, String requestedFormKey) {
        return formAssetCatalog().runtime.buildObjectFieldRegistryFormSchema(object, requestedFormKey);
    }

    private BusinessFormAssetCatalog formAssetCatalog() {
        ApplicationPageFormAssets pages = new ApplicationPageFormAssets(businessApplicationService);
        RuntimeBusinessForms runtime = new RuntimeBusinessForms(crudConfigMapper,
                businessFieldDesignService, pages);
        return new BusinessFormAssetCatalog(businessObjectMapper, codeFormProviderRegistry,
                codeAppMetadataStore(), pages, runtime);
    }

    /**
     * 从业务记录发起流程
     *
     * @param objectCode 业务对象编码
     * @param recordId   业务记录ID
     * @param recordData 业务记录数据
     * @return 流程发起结果
     */
    public JSONObject startFlow(String objectCode, String recordId, Map<String, Object> recordData) {
        if (flowClient == null) {
            throw new RuntimeException("流程服务未配置，无法发起主流程");
        }
        Long tenantId = resolveTenantId();

        // 1. 查询该对象的 FLOW 绑定配置
        AiBusinessBinding flowBinding = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", objectCode, "FLOW");

        if (flowBinding == null || flowBinding.getBindingConfig() == null) {
            throw new RuntimeException(UiMessages.translate("业务对象 [") + objectCode + UiMessages.translate("] 未配置流程绑定"));
        }

        JSONObject bindingConfig = readBindingConfig(flowBinding.getBindingConfig());
        String flowModelKey = bindingConfiguration.resolveFlowModelKey(bindingConfig);

        if (flowModelKey == null || flowModelKey.isBlank()) {
            throw new RuntimeException("流程绑定配置中缺少 flowModelKey");
        }

        // 2. 构建流程变量
        Map<String, Object> flowVariables = buildFlowVariables(bindingConfig, recordData);

        // 3. 构建业务Key和标题
        String businessKey = objectCode + ":" + recordId;
        String title = buildFlowTitle(bindingConfig, recordData, objectCode);

        // 4. 发起流程
        Long userId = resolveUserId();
        String userName = resolveUsername();

        FlowResult<String> result = flowClient.startProcess(
                flowModelKey, businessKey, title,
                flowVariables, String.valueOf(userId), userName, null, null);

        if (!result.isSuccess()) {
            throw new RuntimeException(UiMessages.translate("流程发起失败: ") + result.getMsg());
        }

        JSONObject response = new JSONObject();
        response.put("flowModelKey", flowModelKey);
        response.put("businessKey", businessKey);
        response.put("processInstanceId", result.getData());
        response.put("status", "STARTED");
        return response;
    }

    /**
     * 查询业务对象的流程绑定配置
     */
    public BusinessFlowBindingVO getFlowBinding(String objectCode) {
        Long tenantId = resolveTenantId();
        String canonicalObjectCode = resolveCanonicalObjectCode(tenantId, objectCode);
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, canonicalObjectCode, objectCode);

        if (binding == null) {
            AiBusinessDocumentConfig documentConfig = resolveEnabledDocumentConfig(tenantId, canonicalObjectCode,
                    resolvePublishedRuntimeConfig(tenantId, objectCode));
            if (documentConfig == null || StringUtils.isBlank(documentConfig.getDefaultFlowKey())) {
                return null;
            }
            return bindingPresentation.legacyDocumentFlowToVO(canonicalObjectCode, documentConfig);
        }
        return toVO(canonicalObjectCode, binding);
    }

    /**
     * 查询流程模型变量候选项和字段映射建议。
     */
    public Map<String, Object> getVariableCandidates(String modelKey, String objectCode) {
        if (StringUtils.isBlank(modelKey)) {
            throw new BusinessException("流程模型Key不能为空");
        }
        return variableResolver.resolve(modelKey, objectCode);
    }

    /**
     * 批量补齐流程任务/抄送列表中的业务对象名称和业务摘要。
     */
    public void enrichBusinessListDisplay(List<FlowBusinessListDisplayItem> items) {
        listViews().enrichBusinessListDisplay(items);
    }

    /**
     * 按流程模型 Key 反查已绑定的业务对象。
     */
    public List<BusinessBindingSummaryVO> listBusinessBindingsByModelKey(String modelKey) {
        String key = StringUtils.trimToNull(modelKey);
        if (key == null) {
            throw new BusinessException("流程模型Key不能为空");
        }
        return bindingMapper.selectFlowBindingsByModelKey(resolveTenantId(), key);
    }

    /**
     * 查询业务对象可供流程节点绑定的表单资产。
     */
    public Map<String, Object> getFormAssets(String objectCode) {
        return getFormAssets(objectCode, false);
    }

    /**
     * 查询业务对象可供流程节点绑定的表单资产。
     */
    public Map<String, Object> getFormAssets(String objectCode, boolean includeInternal) {
        return getFormAssets(objectCode, includeInternal, null);
    }

    /**
     * 查询当前应用中当前业务对象实际可作为审批任务表单的页面资产。
     * applicationId 为空时保留对象级/代码表单兼容目录；有应用上下文时只返回该应用的真实页面表单，
     * 避免把业务对象字段注册表误显示成一个不存在的页面。
     */
    public Map<String, Object> getFormAssets(String objectCode,
                                             boolean includeInternal,
                                             Long applicationId) {
        return formAssetCatalog().getFormAssets(objectCode, includeInternal, applicationId);
    }

    /**
     * 从应用草稿中的页面节点和页面布局提取真实页面表单。
     * 表单 key 带应用/页面/资产三段稳定身份，运行时无需额外传 applicationId 即可重新解析页面。
     */

    /**
     * 查询代码应用配置化元数据。未配置时返回空 Map，调用方继续使用 Provider 默认资产。
     */
    public Map<String, Object> getCodeAppMetadata(String objectCode) {
        return codeAppMetadataStore().get(objectCode);
    }

    /**
     * 只更新已有流程绑定中的代码应用元数据，避免字段/视图配置覆盖流程模型和变量映射。
     */
    public boolean saveCodeAppMetadata(String objectCode, Object metadata) {
        return codeAppMetadataStore().save(objectCode, metadata);
    }

    /**
     * 查询待办任务对应的业务表单上下文。
     */
    public BusinessTaskFormContextVO getTaskFormContext(BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormContextQueryDTO effectiveQuery = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        validateTaskAccess(effectiveQuery, false);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, false);
        return buildTaskFormContext(effectiveQuery, runtime);
    }

    /**
     * 查询当前用户已签收、可直接办理的待办上下文。
     *
     * <p>该只读校验入口供受控流程动作在 elicitation 前确认真实办理权使用，
     * 候选但未签收的任务不会被视为可办理任务。</p>
     */
    public BusinessTaskFormContextVO getActionableTaskFormContext(BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormContextQueryDTO effectiveQuery = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        validateTaskAccess(effectiveQuery, true);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, true);
        return buildTaskFormContext(effectiveQuery, runtime);
    }

    /**
     * 查询历史/已办场景下的业务表单上下文，只用于只读展示，不校验运行中待办任务身份。
     */
    public BusinessTaskFormContextVO getTaskFormReadonlyContext(BusinessTaskFormContextQueryDTO query) {
        BusinessTaskFormContextQueryDTO effectiveQuery = query == null ? new BusinessTaskFormContextQueryDTO() : query;
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(effectiveQuery, false);
        BusinessTaskFormContextVO context = buildTaskFormContext(effectiveQuery, runtime);
        makeBusinessTaskFormReadonly(context);
        return context;
    }

    /**
     * 查询当前登录用户本人收到的抄送所关联的业务表单上下文。
     * 收件归属先由流程服务的抄送详情接口校验，再消费其返回的流程/业务身份。
     */
    public BusinessTaskFormContextVO getCcTaskFormReadonlyContext(String ccId) {
        CcTaskFormLookup.VerifiedForm form = CcTaskFormLookup.read(flowClient, ccId);
        BusinessTaskFormContextQueryDTO query = form.query();
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, false);
        BusinessTaskFormContextVO context = buildTaskFormContext(query, runtime, form.data());
        makeBusinessTaskFormReadonly(context);
        return context;
    }

    /**
     * 保存待办任务允许编辑的业务字段，并返回最新上下文。
     */
    public BusinessTaskFormContextVO saveTaskFormContext(BusinessTaskFormSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务待办表单参数不能为空");
        }
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setBusinessKey(dto.getBusinessKey());
        query.setProcessInstanceId(dto.getProcessInstanceId());
        query.setProcessDefKey(dto.getProcessDefKey());
        query.setTaskDefKey(dto.getTaskDefKey());
        query.setObjectCode(dto.getObjectCode());
        query.setRecordId(dto.getRecordId());
        query.setFormKey(dto.getFormKey());

        validateTaskAccess(query, true);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true);
        JSONObject nodeForm = resolveTaskNodeForm(runtime, query);
        if (nodeForm == null || nodeForm.isEmpty()) {
            throw new BusinessException("当前流程节点未配置业务表单权限");
        }
        String formMode = normalizeNodeFormMode(nodeForm.getString("formMode"));
        if ("BUSINESS_CODE_FORM".equals(formMode)) {
            List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
            BusinessTaskFormSaveDTO filteredDto = filterSaveDataByPermissions(dto, permissions);
            validateRequiredTaskFields(permissions, filteredDto.getData(), dto.getData() == null ? Map.of() : dto.getData());
            return saveBusinessCodeFormContext(filteredDto, nodeForm);
        }
        if (!"BUSINESS_OBJECT_FORM".equals(formMode)) {
            throw new BusinessException("当前节点不是平台可保存的业务表单，不能通过平台保存业务字段");
        }
        List<Map<String, Object>> permissions = normalizeFieldPermissions(nodeForm.get("fieldPermissions"));
        if (permissions.isEmpty()) {
            String formKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(query.getFormKey()),
                    StringUtils.trimToNull(nodeForm.getString("formKey")));
            BusinessObjectVO object = queryBusinessObject(resolveTenantId(), runtime.objectCode());
            JSONObject formSchema = resolveBusinessFormSchema(object, formKey, runtime.configKey());
            List<Map<String, Object>> fieldCatalog = resolveBusinessTaskCrudPageFields(runtime.configKey(), formKey, formSchema);
            permissions = normalizeBusinessObjectTaskPermissions(fieldCatalog, permissions);
        }
        Set<String> writableFields = collectPermissionFields(permissions, "writable", true);
        if (writableFields.isEmpty()) {
            throw new BusinessException("当前节点没有可编辑业务字段");
        }

        Map<String, Object> input = dto.getData() == null ? Map.of() : dto.getData();
        Map<String, Object> updateData = new LinkedHashMap<>();
        for (String field : writableFields) {
            if (input.containsKey(field)) {
                updateData.put(field, input.get(field));
            }
        }
        validateRequiredTaskFields(permissions, updateData, input);
        if (updateData.isEmpty()) {
            throw new BusinessException("未提交可编辑业务字段");
        }

        dynamicCrudService.updateInternalFieldsById(runtime.configKey(), runtime.recordId(), updateData);
        return buildTaskFormContext(query, runtime);
    }

    /**
     * 办理低代码业务待办。该入口在 Flowable 任务完成后同步业务流程实例和业务单据状态，
     * 避免低代码单据状态停留在发起时的 IN_PROCESS。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO completeBusinessTask(BusinessTaskActionDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务待办办理参数不能为空");
        }
        String action = StringUtils.defaultIfBlank(dto.getAction(), "approve").trim().toLowerCase();
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new BusinessException("当前业务待办仅支持同意或驳回");
        }
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法办理业务待办");
        }

        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setBusinessKey(dto.getBusinessKey());
        query.setProcessInstanceId(dto.getProcessInstanceId());
        query.setProcessDefKey(dto.getProcessDefKey());
        query.setTaskDefKey(dto.getTaskDefKey());
        query.setObjectCode(dto.getObjectCode());
        query.setRecordId(dto.getRecordId());
        query.setFormKey(dto.getFormKey());

        validateTaskAccess(query, true);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true);
        Map<String, Object> variables = dto.getVariables() == null ? Map.of() : dto.getVariables();
        String userId = String.valueOf(resolveUserId());

        FlowResult<Void> result = "reject".equals(action)
                ? flowClient.reject(query.getTaskId(), userId, dto.getComment(), dto.getSignature(),
                        resolveTrustedTaskTenant(dto), dto.getIdempotencyKey(), dto.getRequestDigest())
                : flowClient.approve(query.getTaskId(), userId, dto.getComment(), dto.getSignature(), variables,
                        resolveTrustedTaskTenant(dto), dto.getIdempotencyKey(), dto.getRequestDigest());
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null
                    ? "业务待办办理失败"
                    : StringUtils.defaultIfBlank(result.getMsg(), "业务待办办理失败"));
        }

        return syncBusinessFlowStatusAfterTaskAction(runtime, query, action, variables);
    }

    /**
     * 仅供 FLOW_ACTION 本地审计恢复调用。此时远程任务可能已完成，不能再要求它处于 actionable；
     * Flow 服务仍以同租户、原签收人、动作、幂等键和请求摘要做最终裁决。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO recoverCapabilityTaskAction(BusinessTaskActionDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getTaskId())
                || StringUtils.isBlank(dto.getIdempotencyKey())
                || StringUtils.isBlank(dto.getRequestDigest())) {
            throw new BusinessException(409, "FLOW_RECOVERY_EVIDENCE_REQUIRED");
        }
        String action = StringUtils.defaultIfBlank(dto.getAction(), "approve").trim().toLowerCase();
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new BusinessException(409, "POLICY_MISMATCH");
        }
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法恢复业务待办");
        }
        Long tenantId = resolveTrustedTaskTenant(dto);
        String userId = String.valueOf(resolveUserId());
        Map<String, Object> variables = Map.of();
        FlowResult<Void> result = "reject".equals(action)
                ? flowClient.reject(dto.getTaskId(), userId, dto.getComment(), dto.getSignature(),
                        tenantId, dto.getIdempotencyKey(), dto.getRequestDigest())
                : flowClient.approve(dto.getTaskId(), userId, dto.getComment(), dto.getSignature(), variables,
                        tenantId, dto.getIdempotencyKey(), dto.getRequestDigest());
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null
                    ? "业务待办恢复失败"
                    : StringUtils.defaultIfBlank(result.getMsg(), "业务待办恢复失败"));
        }

        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setObjectCode(dto.getObjectCode());
        query.setRecordId(dto.getRecordId());
        if (StringUtils.isNotBlank(dto.getObjectCode()) && dto.getRecordId() != null) {
            String objectCode = resolveCanonicalObjectCode(tenantId, dto.getObjectCode());
            query.setBusinessKey(buildBusinessKey(objectCode, dto.getRecordId()));
        }
        return syncBusinessFlowStatusAfterTaskAction(null, query, action, variables);
    }

    private Long resolveTrustedTaskTenant(BusinessTaskActionDTO dto) {
        Long currentTenantId = resolveTenantId();
        if (dto.getTenantId() != null && !dto.getTenantId().equals(currentTenantId)) {
            throw new BusinessException(403, "FLOW_TASK_TENANT_MISMATCH");
        }
        return currentTenantId;
    }

    private BusinessFlowRuntimeVO syncBusinessFlowStatusAfterTaskAction(TaskFormRuntimeContext runtime,
                                                                        BusinessTaskFormContextQueryDTO query,
                                                                        String action,
                                                                        Map<String, Object> variables) {
        String businessKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getBusinessKey()),
                runtime == null ? null : StringUtils.trimToNull(runtime.businessKey()));
        String processInstanceId = StringUtils.trimToNull(query.getProcessInstanceId());
        AiBusinessFlowInstanceLink link = findRuntimeLink(resolveTenantId(), processInstanceId, businessKey);
        if (link == null) {
            BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
            vo.setObjectCode(runtime == null ? null : runtime.objectCode());
            vo.setRecordId(runtime == null ? null : runtime.recordId());
            vo.setBusinessKey(businessKey);
            vo.setProcessInstanceId(processInstanceId);
            vo.setFlowStatus("IN_PROCESS");
            vo.setMessage("业务待办已办理，未找到低代码流程实例关联");
            return vo;
        }

        String engineStatus = readFlowEngineBusinessStatus(resolveFlowEngineBusinessKey(link));
        String terminalResult = resolveTerminalBusinessFlowResult(engineStatus);
        if (StringUtils.isNotBlank(terminalResult)) {
            BusinessFlowCallbackDTO callback = new BusinessFlowCallbackDTO();
            callback.setProcessInstanceId(StringUtils.firstNonBlank(processInstanceId, link.getProcessInstanceId()));
            callback.setBusinessKey(link.getBusinessKey());
            callback.setResult(terminalResult);
            callback.setFlowStatus(engineStatus);
            callback.setTenantId(link.getTenantId());
            callback.setOperatorId(resolveUserId());
            callback.setVariables(variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variables));
            handleFlowCallbackInternal(link, callback);
            return toRuntimeVO(link, "业务待办已办理，流程已结束");
        }

        if (!"IN_PROCESS".equalsIgnoreCase(link.getFlowStatus())) {
            link.setFlowStatus("IN_PROCESS");
            flowInstanceLinkMapper.updateById(link);
        }
        return toRuntimeVO(link, "业务待办已办理，流程继续流转");
    }

    private String readFlowEngineBusinessStatus(String businessKey) {
        if (flowClient == null || StringUtils.isBlank(businessKey)) {
            return null;
        }
        try {
            FlowResult<Map<String, Object>> status = flowClient.getProcessStatus(businessKey);
            if (status == null || !status.isSuccess() || status.getData() == null) {
                return null;
            }
            return StringUtils.trimToNull(textValue(status.getData().get("status")));
        } catch (Exception e) {
            log.debug("[低代码流程状态] 读取 Flowable 业务状态失败: businessKey={}, error={}",
                    businessKey, e.getMessage());
            return null;
        }
    }

    private String resolveFlowEngineBusinessKey(AiBusinessFlowInstanceLink link) {
        if (link == null) {
            return null;
        }
        JSONObject variables = readJsonObject(link.getVariablesSnapshot());
        return StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(variables.get("flowBusinessKey"))),
                StringUtils.trimToNull(link.getBusinessKey()));
    }

    private String resolveTerminalBusinessFlowResult(String engineStatus) {
        String normalized = StringUtils.trimToEmpty(engineStatus).toUpperCase();
        if (normalized.contains("APPROVED") || normalized.contains("COMPLETED")) {
            return "APPROVED";
        }
        if (normalized.contains("REJECT")) {
            return "REJECTED";
        }
        if (normalized.contains("CANCEL") || normalized.contains("TERMINAT") || normalized.contains("WITHDRAW")) {
            return "CANCELED";
        }
        return null;
    }

    /**
     * 驳回修改后重提。复杂代码业务可先在业务页保存主数据，再调用该接口完成修改节点。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO resubmit(BusinessFlowResubmitDTO dto) {
        if (dto == null) {
            throw new BusinessException("重提参数不能为空");
        }
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(dto.getTaskId());
        query.setBusinessKey(dto.getBusinessKey());
        query.setProcessInstanceId(dto.getProcessInstanceId());
        query.setProcessDefKey(dto.getProcessDefKey());
        query.setTaskDefKey(dto.getTaskDefKey());

        validateTaskAccess(query, true);
        TaskFormRuntimeContext runtime = resolveTaskFormRuntimeContext(query, true);
        Map<String, Object> variables = dto.getVariables() == null ? Map.of() : dto.getVariables();
        FlowResult<Void> result = flowClient.approve(
                query.getTaskId(),
                String.valueOf(resolveUserId()),
                StringUtils.defaultIfBlank(dto.getComment(), "修改后重提"),
                variables);
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null ? "重提失败" : StringUtils.defaultIfBlank(result.getMsg(), "重提失败"));
        }

        AiBusinessFlowInstanceLink link = findRuntimeLink(resolveTenantId(), query.getProcessInstanceId(), runtime.businessKey());
        if (link == null) {
            BusinessFlowRuntimeVO vo = new BusinessFlowRuntimeVO();
            vo.setObjectCode(runtime.objectCode());
            vo.setRecordId(runtime.recordId());
            vo.setBusinessKey(runtime.businessKey());
            vo.setProcessInstanceId(query.getProcessInstanceId());
            vo.setFlowStatus("IN_PROCESS");
            vo.setMessage("已重提");
            return vo;
        }

        AiBusinessDocumentConfig documentConfig = documentConfigService.selectEnabledByObjectCode(
                link.getTenantId(), link.getObjectCode());
        AiCrudConfig runtimeConfig = documentConfig == null
                ? resolvePublishedRuntimeConfig(link.getTenantId(), link.getObjectCode())
                : null;
        AiBusinessBinding binding = selectMainFlowBindingForConfig(link.getTenantId(), link.getObjectCode());
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, link.getTenantId(), link.getObjectCode());
        statusUpdates().update(documentConfig, runtimeConfig, bindingConfig, link.getRecordId(), "IN_PROCESS");

        link.setFlowStatus("IN_PROCESS");
        link.setResult(null);
        link.setEndTime(null);
        if (!variables.isEmpty()) {
            link.setVariablesSnapshot(JSON.toJSONString(variables));
        }
        flowInstanceLinkMapper.updateById(link);
        return toRuntimeVO(link, "已重提");
    }

    private void validateTaskAccess(BusinessTaskFormContextQueryDTO query, boolean writeRequired) {
        taskAuthorization.validate(query, writeRequired, flowClient, this::resolveUserId);
    }



    private BusinessTaskFormContextVO buildTaskFormContext(BusinessTaskFormContextQueryDTO query,
                                                           TaskFormRuntimeContext runtime) {
        return buildTaskFormContext(query, runtime, null);
    }

    private BusinessTaskFormContextVO buildTaskFormContext(BusinessTaskFormContextQueryDTO query,
                                                           TaskFormRuntimeContext runtime,
                                                           Map<String, Object> trustedFlowFormInfo) {
        BusinessTaskFormContextVO vo = new BusinessTaskFormContextVO();
        vo.setTaskId(StringUtils.trimToNull(query.getTaskId()));
        vo.setBusinessKey(runtime.businessKey());
        vo.setProcessInstanceId(StringUtils.trimToNull(query.getProcessInstanceId()));
        vo.setProcessDefKey(StringUtils.trimToNull(query.getProcessDefKey()));
        vo.setTaskDefKey(StringUtils.trimToNull(query.getTaskDefKey()));
        vo.setObjectCode(runtime.objectCode());
        vo.setRecordId(runtime.recordId());
        vo.setConfigKey(runtime.configKey());
        vo.setFormType("none");

        if (StringUtils.isBlank(runtime.objectCode()) || runtime.recordId() == null) {
            vo.getWarnings().add("未解析到业务对象或记录");
            return vo;
        }

        JSONObject nodeForm = resolveTaskNodeForm(runtime, query, trustedFlowFormInfo);
        if (nodeForm == null || nodeForm.isEmpty()) {
            vo.getWarnings().add("当前节点未配置业务表单策略");
            return vo;
        }
        String formMode = normalizeNodeFormMode(nodeForm.getString("formMode"));
        if (!"BUSINESS_OBJECT_FORM".equals(formMode)) {
            if ("BUSINESS_CODE_FORM".equals(formMode)) {
                return buildBusinessCodeFormContext(query, nodeForm, runtime);
            }
            vo.setFormType(formMode);
            vo.setFormKey(StringUtils.trimToNull(nodeForm.getString("formKey")));
            vo.setFormName(StringUtils.trimToNull(nodeForm.getString("formName")));
            vo.setProviderKey(StringUtils.trimToNull(nodeForm.getString("providerKey")));
            vo.setFormUrl(StringUtils.trimToNull(nodeForm.getString("formUrl")));
            vo.setEditMode(normalizeNodeEditMode(nodeForm.getString("editMode")));
            vo.setFormRef(readNestedObject(nodeForm.get("formRef")));
            applyPageFormIdentity(vo, vo.getFormRef());
            applyApprovalPolicy(vo, nodeForm);
            vo.getWarnings().add("当前节点表单类型暂不由低代码业务表单渲染: " + formMode);
            return vo;
        }
        if (StringUtils.isBlank(runtime.configKey())) {
            vo.getWarnings().add("业务对象缺少已发布运行配置，无法加载低代码业务表单");
            return vo;
        }

        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getFormKey()),
                StringUtils.trimToNull(nodeForm.getString("formKey")));
        return objectTaskForms().read(vo, nodeForm, formKey, resolveTenantId(), runtime.bindingConfig());
    }

    private void applyPageFormIdentity(BusinessTaskFormContextVO vo, Map<String, Object> formRef) {
        taskFormProjection.applyPageFormIdentity(vo, formRef);
    }

    private BusinessObjectTaskFormReader objectTaskForms() {
        return new BusinessObjectTaskFormReader(runtimeMetadata(), formAssetCatalog(), dynamicCrudService,
                taskFormProjection, codeTaskFields);
    }

    private List<Map<String, Object>> resolveBusinessTaskCrudPageFields(String configKey,
                                                                        String formKey,
                                                                        JSONObject formSchema) {
        return objectTaskForms().resolveBusinessTaskCrudPageFields(configKey, formKey, formSchema);
    }

    private void makeBusinessTaskFormReadonly(BusinessTaskFormContextVO context) {
        if (context == null) {
            return;
        }
        taskFormProjection.makeBusinessTaskFormReadonly(context);
        context.setAllowApprove(false);
        context.setAllowReject(false);
        context.setAllowRejectToStart(false);
        context.setAllowDelegate(false);
        context.setAllowReturn(false);
        context.setAllowTerminate(false);
        context.setRequireComment(false);
        context.setRequireSignature(false);
    }

    private BusinessTaskFormContextVO buildBusinessCodeFormContext(BusinessTaskFormContextQueryDTO query,
                                                                   JSONObject nodeForm,
                                                                   TaskFormRuntimeContext runtime) {
        BusinessCodeTaskContext.Result result = codeTaskContext().read(query, nodeForm,
                new BusinessCodeTaskContext.Identity(runtime.objectCode(), runtime.recordId(),
                        runtime.businessKey(), runtime.configKey()), readNestedObject(nodeForm.get("formRef")));
        return result.providerInvoked()
                ? applyBusinessCodeResult(result.context(), runtime.objectCode(), result.permissions()) : result.context();
    }

    private BusinessTaskFormContextVO saveBusinessCodeFormContext(BusinessTaskFormSaveDTO dto, JSONObject nodeForm) {
        BusinessCodeTaskContext.Result result = codeTaskContext().save(dto, nodeForm,
                readNestedObject(nodeForm.get("formRef")));
        return applyBusinessCodeResult(result.context(), dto.getObjectCode(), result.permissions());
    }

    private BusinessCodeTaskContext codeTaskContext() {
        return new BusinessCodeTaskContext(codeFormProviderRegistry, codeTaskFields);
    }

    private void applyApprovalPolicy(BusinessTaskFormContextVO vo, JSONObject source) {
        codeTaskFields.applyApprovalPolicy(vo, source);
    }

    private BusinessTaskFormContextVO applyBusinessCodeFieldPermissions(BusinessTaskFormContextVO context,
                                                                        List<Map<String, Object>> permissions) {
        return taskFormProjection.applyBusinessCodeFieldPermissions(context, permissions);
    }

    private BusinessTaskFormSaveDTO filterSaveDataByPermissions(BusinessTaskFormSaveDTO dto,
                                                                List<Map<String, Object>> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            throw new BusinessException("当前节点没有可编辑业务字段");
        }
        Set<String> writableFields = collectPermissionFields(permissions, "writable", true);
        if (writableFields.isEmpty()) {
            throw new BusinessException("当前节点没有可编辑业务字段");
        }
        Map<String, Object> input = dto.getData() == null ? Map.of() : dto.getData();
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (String field : writableFields) {
            if (input.containsKey(field)) {
                filteredData.put(field, input.get(field));
            }
        }
        BusinessTaskFormSaveDTO filtered = new BusinessTaskFormSaveDTO();
        filtered.setTaskId(dto.getTaskId());
        filtered.setBusinessKey(dto.getBusinessKey());
        filtered.setProcessInstanceId(dto.getProcessInstanceId());
        filtered.setProcessDefKey(dto.getProcessDefKey());
        filtered.setTaskDefKey(dto.getTaskDefKey());
        filtered.setObjectCode(dto.getObjectCode());
        filtered.setRecordId(dto.getRecordId());
        filtered.setFormKey(dto.getFormKey());
        filtered.setData(filteredData);
        return filtered;
    }

    private TaskFormRuntimeContext resolveTaskFormRuntimeContext(BusinessTaskFormContextQueryDTO query, boolean strict) {
        Long tenantId = resolveTenantId();
        AiBusinessFlowInstanceLink link = null;
        if (StringUtils.isNotBlank(query.getProcessInstanceId())) {
            link = flowInstanceLinkMapper.selectByProcessInstanceId(tenantId, query.getProcessInstanceId());
        }
        if (link == null && StringUtils.isNotBlank(query.getBusinessKey())) {
            link = flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, query.getBusinessKey());
        }

        String objectCode = StringUtils.firstNonBlank(
                link == null ? null : link.getObjectCode(),
                StringUtils.trimToNull(query.getObjectCode()),
                parseBusinessKeyObjectCode(query.getBusinessKey()));
        Long recordId = link == null || link.getRecordId() == null
                ? query.getRecordId()
                : link.getRecordId();
        if (recordId == null) {
            recordId = parseBusinessKeyRecordId(query.getBusinessKey());
        }
        String businessKey = StringUtils.firstNonBlank(
                link == null ? null : link.getBusinessKey(),
                StringUtils.trimToNull(query.getBusinessKey()),
                objectCode != null && recordId != null ? buildBusinessKey(objectCode, recordId) : null);

        if (StringUtils.isBlank(objectCode) || recordId == null) {
            if (strict) {
                throw new BusinessException("未解析到业务对象或记录ID");
            }
            return new TaskFormRuntimeContext(null, null, businessKey, null, null);
        }

        BusinessRuntimeContext businessContext = resolveBusinessRuntimeContext(tenantId, objectCode);
        String canonicalObjectCode = StringUtils.firstNonBlank(businessContext.objectCode(), objectCode);
        String configKey = businessContext.configKey();
        AiBusinessBinding binding = selectMainFlowBindingForConfig(tenantId, canonicalObjectCode, objectCode);
        JSONObject bindingConfig = binding == null ? new JSONObject() : readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(bindingConfig, tenantId, canonicalObjectCode);

        if (StringUtils.isBlank(configKey) && strict && !isBusinessCodeTaskForm(canonicalObjectCode, bindingConfig, query)) {
            throw new BusinessException("业务对象缺少已发布运行配置，无法保存待办业务字段");
        }
        return new TaskFormRuntimeContext(canonicalObjectCode, recordId, businessKey, configKey, bindingConfig);
    }

    private boolean isBusinessCodeTaskForm(String objectCode, JSONObject bindingConfig, BusinessTaskFormContextQueryDTO query) {
        JSONObject nodeForm = resolveTaskNodeForm(
                new TaskFormRuntimeContext(objectCode, null, null, null, bindingConfig), query);
        return nodeForm != null && "BUSINESS_CODE_FORM".equals(normalizeNodeFormMode(nodeForm.getString("formMode")));
    }

    private BusinessObjectVO queryBusinessObject(Long tenantId, String objectCode) {
        return runtimeMetadata().queryBusinessObject(tenantId, objectCode);
    }

    private JSONObject resolveTaskNodeForm(TaskFormRuntimeContext runtime, BusinessTaskFormContextQueryDTO query) {
        return resolveTaskNodeForm(runtime, query, null);
    }

    private JSONObject resolveTaskNodeForm(TaskFormRuntimeContext runtime,
                                           BusinessTaskFormContextQueryDTO query,
                                           Map<String, Object> trustedFlowFormInfo) {
        JSONObject flowNodeForm = resolveFlowNodeForm(runtime, query, trustedFlowFormInfo);
        if (!flowNodeForm.isEmpty()) {
            return flowNodeForm;
        }
        return FlowNodeFormResolution.findBindingNode(runtime.bindingConfig(), query.getTaskDefKey());
    }

    private JSONObject resolveFlowNodeForm(TaskFormRuntimeContext runtime, BusinessTaskFormContextQueryDTO query) {
        return resolveFlowNodeForm(runtime, query, null);
    }

    private JSONObject resolveFlowNodeForm(TaskFormRuntimeContext runtime,
                                           BusinessTaskFormContextQueryDTO query,
                                           Map<String, Object> trustedFlowFormInfo) {
        String objectCode = StringUtils.trimToNull(runtime.objectCode());
        if (StringUtils.isBlank(objectCode)) {
            return new JSONObject();
        }
        Map<String, Object> formInfo = trustedFlowFormInfo == null
                ? new FlowNodeFormReader(flowClient).load(runtime.businessKey(), runtime.bindingConfig(), query)
                : trustedFlowFormInfo;
        FlowNodeFormResolution resolution = new FlowNodeFormResolution(runtime.configKey(), formInfo, query);
        JSONObject runtimeAsset = resolveBusinessTaskFormAsset(objectCode, resolution.runtimeFormKey());
        resolution.selectRuntimeAsset(runtimeAsset);
        JSONObject asset = resolveBusinessTaskFormAsset(objectCode, resolution.formKey());
        return resolution.resolve(asset);
    }

    private JSONObject resolveBusinessTaskFormAsset(String objectCode, String formKey) {
        return formAssetCatalog().resolveBusinessTaskFormAsset(objectCode, formKey);
    }

    private JSONObject resolveBusinessFormSchema(BusinessObjectVO object, String formKey, String configKey) {
        return formAssetCatalog().runtime.resolveBusinessFormSchema(object, formKey, configKey);
    }

    private CodeAppMetadataStore codeAppMetadataStore() {
        return new CodeAppMetadataStore(bindingMapper, runtimeMetadata());
    }

    private Set<String> readStringSet(Object source) {
        Set<String> result = new LinkedHashSet<>();
        if (source == null) {
            return result;
        }
        if (source instanceof String text && !StringUtils.trimToEmpty(text).startsWith("[")) {
            for (String item : text.split(",")) {
                String value = StringUtils.trimToNull(item);
                if (value != null) {
                    result.add(value);
                }
            }
            return result;
        }
        JSONArray array = readNestedArray(source);
        for (int i = 0; i < array.size(); i++) {
            String value = StringUtils.trimToNull(textValue(array.get(i)));
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private List<Map<String, Object>> normalizeCodeAppMetadataFields(Object source, boolean includeInternal) {
        return CodeFormMetadata.normalizeCodeAppMetadataFields(source, includeInternal);
    }

    private boolean isPublicCodeAppFormField(Map<String, Object> field) {
        return CodeFormMetadata.isPublicCodeAppFormField(field);
    }

    private BusinessTaskFormContextVO applyBusinessCodeResult(BusinessTaskFormContextVO context, String objectCode,
                                                              List<Map<String, Object>> permissions) {
        if (context == null) {
            return null;
        }
        String code = StringUtils.firstNonBlank(StringUtils.trimToNull(objectCode),
                StringUtils.trimToNull(context.getObjectCode()));
        JSONObject metadata = codeAppMetadataStore().read(resolveTenantId(), code);
        return codeTaskFields.apply(context, metadata, permissions);
    }

    private List<Map<String, Object>> buildTaskFormFields(List<Map<String, Object>> fieldCatalog,
                                                          List<Map<String, Object>> permissions) {
        return taskFormProjection.buildTaskFormFields(fieldCatalog, permissions);
    }

    private String resolveBusinessObjectTaskEditMode(JSONObject nodeForm, List<Map<String, Object>> permissions) {
        return taskFormProjection.resolveBusinessObjectTaskEditMode(nodeForm, permissions);
    }

    private List<Map<String, Object>> normalizeBusinessObjectTaskPermissions(List<Map<String, Object>> fieldCatalog,
                                                                             List<Map<String, Object>> permissions) {
        return taskFormProjection.normalizeBusinessObjectTaskPermissions(fieldCatalog, permissions);
    }

    private String normalizeTaskFormFieldType(String componentType) {
        return taskFormProjection.normalizeTaskFormFieldType(componentType);
    }

    private Map<String, Object> filterVisibleRecordData(Map<String, Object> recordData, List<Map<String, Object>> fields) {
        return taskFormProjection.filterVisibleRecordData(recordData, fields);
    }

    private void putPermissionAliases(Map<String, Map<String, Object>> permissionMap,
                                      String field,
                                      Map<String, Object> permission) {
        taskFormProjection.putPermissionAliases(permissionMap, field, permission);
    }

    private Set<String> collectPermissionFields(List<Map<String, Object>> permissions, String permissionKey, boolean expected) {
        Set<String> result = new LinkedHashSet<>();
        if (permissions == null) {
            return result;
        }
        for (Map<String, Object> permission : permissions) {
            if (readBooleanValue(permission.get(permissionKey), false) == expected) {
                String field = StringUtils.trimToNull(textValue(permission.get("field")));
                if (field != null) {
                    result.add(field);
                }
            }
        }
        return result;
    }

    private void validateRequiredTaskFields(List<Map<String, Object>> permissions,
                                            Map<String, Object> updateData,
                                            Map<String, Object> input) {
        if (permissions == null) {
            return;
        }
        for (Map<String, Object> permission : permissions) {
            boolean required = readBooleanValue(permission.get("required"), false);
            boolean writable = readBooleanValue(permission.get("writable"), false);
            if (!required || !writable) {
                continue;
            }
            String field = StringUtils.trimToNull(textValue(permission.get("field")));
            if (field == null) {
                continue;
            }
            if (!input.containsKey(field)) {
                throw new BusinessException(UiMessages.translate("请填写必填字段: ") + field);
            }
            Object value = updateData.get(field);
            if (value == null || StringUtils.isBlank(String.valueOf(value))) {
                throw new BusinessException(UiMessages.translate("请填写必填字段: ") + field);
            }
        }
    }

    private String parseBusinessKeyObjectCode(String businessKey) {
        return BusinessFlowRuntimeMetadata.parseBusinessKeyObjectCode(businessKey);
    }

    private Long parseBusinessKeyRecordId(String businessKey) {
        return BusinessFlowRuntimeMetadata.parseBusinessKeyRecordId(businessKey);
    }

    private Long parseLongValue(String value) {
        String text = StringUtils.trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 旧触发器路径兼容返回，保留 config 包装结构。
     */
    public JSONObject getFlowBindingLegacy(String objectCode) {
        BusinessFlowBindingVO binding = getFlowBinding(objectCode);
        if (binding == null) {
            return null;
        }
        JSONObject result = new JSONObject();
        result.put("bindingId", binding.getBindingId());
        result.put("bindingName", StringUtils.defaultIfBlank(binding.getFlowModelName(), binding.getFlowModelKey()));
        result.put("objectCode", binding.getObjectCode());
        result.put("status", binding.getStatus());
        result.put("config", bindingConfiguration.toConfigJson(binding));
        return result;
    }

    /**
     * 保存流程绑定配置
     */
    public void saveFlowBinding(String objectCode, BusinessFlowBindingDTO dto) {
        if (dto == null) {
            throw new BusinessException("流程绑定配置不能为空");
        }
        Long tenantId = resolveTenantId();
        String canonicalObjectCode = resolveCanonicalObjectCode(tenantId, objectCode);
        JSONObject config = bindingConfiguration.normalizeBindingConfig(dto);
        ensureBusinessBinding(config, tenantId, canonicalObjectCode);
        String flowModelKey = config.getString("flowModelKey");
        if (StringUtils.isBlank(flowModelKey)) {
            throw new BusinessException("流程模型Key不能为空");
        }
        AiBusinessBinding existing = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", canonicalObjectCode, "FLOW");

        if (existing != null) {
            existing.setTargetType("OBJECT");
            existing.setTargetCode(canonicalObjectCode);
            existing.setBindingType("FLOW");
            existing.setBindingConfig(config.toJSONString());
            existing.setBindingKey(flowModelKey);
            existing.setBindingName(resolveBindingName(config));
            existing.setStatus(1);
            bindingMapper.updateById(existing);
            log.info("[低代码流程绑定] 更新主流程绑定: tenantId={}, objectCode={}, bindingId={}, flowModelKey={}",
                    tenantId, objectCode, existing.getId(), flowModelKey);
        } else {
            AiBusinessBinding binding = new AiBusinessBinding();
            binding.setTenantId(tenantId);
            binding.setTargetType("OBJECT");
            binding.setTargetCode(canonicalObjectCode);
            binding.setBindingType("FLOW");
            binding.setBindingKey(flowModelKey);
            binding.setBindingName(resolveBindingName(config));
            binding.setBindingConfig(config.toJSONString());
            binding.setStatus(1);
            binding.setSortOrder(0);
            bindingMapper.insert(binding);
            log.info("[低代码流程绑定] 创建主流程绑定: tenantId={}, objectCode={}, bindingId={}, flowModelKey={}",
                    tenantId, objectCode, binding.getId(), flowModelKey);
        }
        documentConfigService.syncDefaultFlowKeyByObjectCode(tenantId, canonicalObjectCode, flowModelKey);
    }

    /**
     * 旧触发器路径保存兼容，读取 field/variable 后只落 formField/flowVariable。
     */
    public void saveFlowBinding(String objectCode, JSONObject config) {
        saveFlowBinding(objectCode, bindingConfiguration.toDTO(config));
    }

    /**
     * 手动按钮发起单据流程。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlow(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startDocumentFlowInternal(dto, true, null, null, tenantId, false));
    }

    /**
     * 受控 FLOW_ACTION 专用发起入口。业务 key 永远固定为 objectCode:recordId，
     * 远程成功而本地回填失败时，同 key 重试可恢复原流程实例。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlowForCapability(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startDocumentFlowInternal(dto, true, null, null, tenantId, true));
    }

    /**
     * 旧审批入口兼容发起。接口层仍有旧权限校验，这里不再重复要求新流程按钮权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startDocumentFlowForCompatibility(BusinessFlowStartDTO dto) {
        Long tenantId = resolveTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> startDocumentFlowInternal(dto, false, null, null, tenantId, false));
    }

    /**
     * 由触发器调用的流程发起（内部方法）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startFlowFromTrigger(String flowModelKey, String businessKey, String title,
                                                      Long userId, String userName, JSONObject variables) {
        return startFlowFromTrigger(flowModelKey, businessKey, title, userId, userName, resolveTenantId(), variables);
    }

    /**
     * 由触发器调用的流程发起（内部方法）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startFlowFromTrigger(String flowModelKey, String businessKey, String title,
                                                      Long userId, String userName, Long tenantId, JSONObject variables) {
        BusinessKeyParts parts = parseBusinessKey(businessKey);
        BusinessFlowStartDTO dto = new BusinessFlowStartDTO();
        dto.setObjectCode(parts.objectCode());
        dto.setRecordId(parts.recordId());
        dto.setFlowModelKey(flowModelKey);
        dto.setTitle(title);
        if (variables != null) {
            dto.setVariables(new LinkedHashMap<>(variables));
        }
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        return TenantContextHolder.executeWithTenant(effectiveTenantId,
                () -> startDocumentFlowInternal(dto, false, userId, userName, effectiveTenantId, false, false));
    }

    /**
     * 应用级业务流程审批节点发起 Flowable。显式模型 Key 来自已发布/草稿画布，
     * 业务对象运行配置允许尚未发布的工作台草稿。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessFlowRuntimeVO startFromBusinessProcess(String flowModelKey, String businessKey, String title,
                                                          Long userId, String userName, Long tenantId, JSONObject variables) {
        if (StringUtils.isBlank(flowModelKey)) {
            throw new BusinessException("审批节点未配置已发布流程模型");
        }
        BusinessKeyParts parts = parseBusinessKey(businessKey);
        BusinessFlowStartDTO dto = new BusinessFlowStartDTO();
        dto.setObjectCode(parts.objectCode());
        dto.setRecordId(parts.recordId());
        dto.setFlowModelKey(flowModelKey);
        dto.setTitle(title);
        if (variables != null) {
            dto.setVariables(new LinkedHashMap<>(variables));
        }
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        return TenantContextHolder.executeWithTenant(effectiveTenantId,
                () -> startDocumentFlowInternal(dto, false, userId, userName, effectiveTenantId, false, true));
    }

    /**
     * 查询单据流程状态。
     */
    public BusinessFlowRuntimeVO getFlowStatus(String objectCode, Long recordId) {
        return statusReader().getFlowStatus(objectCode, recordId);
    }

    /**
     * 处理流程引擎回调，按流程结果回写单据状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleFlowCallback(BusinessFlowCallbackDTO dto) {
        if (dto == null || (StringUtils.isBlank(dto.getProcessInstanceId()) && StringUtils.isBlank(dto.getBusinessKey()))) {
            throw new BusinessException("流程回调缺少流程实例ID或业务Key");
        }
        Long tenantId = dto.getTenantId() != null ? dto.getTenantId() : resolveTenantId();
        AiBusinessFlowInstanceLink link = findCallbackLink(tenantId, dto);
        if (link == null) {
            throw new BusinessException("未找到流程实例关联");
        }
        Long effectiveTenantId = link.getTenantId() != null ? link.getTenantId() : tenantId;
        TenantContextHolder.executeWithTenant(effectiveTenantId, () -> handleFlowCallbackInternal(link, dto));
    }

    @FlowCallback(on = {
            FlowCallback.ON_COMPLETED,
            FlowCallback.ON_REJECTED,
            FlowCallback.ON_CANCELED
    })
    @Transactional(rollbackFor = Exception.class)
    public void handleFlowEngineEvent(FlowEventContext ctx) {
        if (ctx == null) {
            return;
        }
        BusinessFlowCallbackDTO dto = new BusinessFlowCallbackDTO();
        dto.setProcessInstanceId(StringUtils.trimToNull(ctx.getProcessInstanceId()));
        dto.setBusinessKey(StringUtils.trimToNull(ctx.getBusinessKey()));
        dto.setFlowStatus(ctx.getEvent());
        dto.setResult(resolveFlowEventResult(ctx.getEvent()));
        dto.setTenantId(ctx.getTenantId());
        dto.setNodeKey(ctx.getTaskDefKey());
        dto.setNodeName(ctx.getTaskName());
        dto.setOperatorId(parseLongValue(ctx.getAssigneeId()));
        dto.setVariables(ctx.getVariables() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(ctx.getVariables()));
        Long tenantId = dto.getTenantId() != null ? dto.getTenantId() : resolveTenantId();
        AiBusinessFlowInstanceLink link = findCallbackLink(tenantId, dto);
        if (link == null) {
            log.debug("[低代码流程回调] 忽略未绑定业务对象的流程事件: event={}, processInstanceId={}, businessKey={}",
                    ctx.getEvent(), ctx.getProcessInstanceId(), ctx.getBusinessKey());
            return;
        }
        Long effectiveTenantId = link.getTenantId() != null ? link.getTenantId() : tenantId;
        try {
            TenantContextHolder.executeWithTenant(effectiveTenantId, () -> handleFlowCallbackInternal(link, dto));
        } catch (Exception e) {
            log.warn("[低代码流程回调] 处理流程事件失败: event={}, processInstanceId={}, businessKey={}, error={}",
                    ctx.getEvent(), ctx.getProcessInstanceId(), ctx.getBusinessKey(), e.getMessage());
            throw e;
        }
    }

    private String resolveFlowEventResult(String event) {
        if (FlowCallback.ON_REJECTED.equals(event)) {
            return "REJECTED";
        }
        if (FlowCallback.ON_CANCELED.equals(event)) {
            return "CANCELED";
        }
        if (FlowCallback.ON_COMPLETED.equals(event)) {
            return "APPROVED";
        }
        return event;
    }

    private BusinessFlowRuntimeVO startDocumentFlowInternal(BusinessFlowStartDTO dto,
                                                            boolean checkPermission,
                                                            Long starterUserId,
                                                            String starterUserName,
                                                            Long tenantId,
                                                            boolean stableBusinessKey) {
        return startDocumentFlowInternal(dto, checkPermission, starterUserId, starterUserName,
                tenantId, stableBusinessKey, false);
    }

    private BusinessFlowRuntimeVO startDocumentFlowInternal(BusinessFlowStartDTO dto,
                                                            boolean checkPermission,
                                                            Long starterUserId,
                                                            String starterUserName,
                                                            Long tenantId,
                                                            boolean stableBusinessKey,
                                                            boolean allowDraftRuntime) {
        return flowStarts.start(new BusinessFlowStartCoordinator.StartRequest(dto, checkPermission,
                starterUserId, starterUserName, tenantId, stableBusinessKey, allowDraftRuntime), flowClient);
    }

    private void handleFlowCallbackInternal(AiBusinessFlowInstanceLink link, BusinessFlowCallbackDTO dto) {
        callbackResults().handle(link, dto);
    }

    private BusinessFlowCallbackResults callbackResults() {
        return new BusinessFlowCallbackResults(runtimeMetadata(), documentConfigService,
                flowInstanceLinkMapper, dynamicCrudService, statusUpdates(),
                applicationEventPublisher, actionExecutionServiceProvider);
    }

    private AiBusinessFlowInstanceLink findCallbackLink(Long tenantId, BusinessFlowCallbackDTO dto) {
        return callbackResults().findCallbackLink(tenantId, dto);
    }

    private AiBusinessFlowInstanceLink findRuntimeLink(Long tenantId, String processInstanceId, String businessKey) {
        if (StringUtils.isNotBlank(processInstanceId)) {
            AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectByProcessInstanceId(tenantId, processInstanceId);
            if (link != null) {
                return link;
            }
        }
        if (StringUtils.isNotBlank(businessKey)) {
            return flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, businessKey);
        }
        return null;
    }

    private AiCrudConfig resolvePublishedRuntimeConfig(Long tenantId, String objectCodeOrConfigKey) {
        return runtimeMetadata().resolvePublishedRuntimeConfig(tenantId, objectCodeOrConfigKey);
    }

    private AiCrudConfig resolveRuntimeConfig(Long tenantId, String objectCodeOrConfigKey) {
        return runtimeMetadata().resolveRuntimeConfig(tenantId, objectCodeOrConfigKey);
    }

    private AiBusinessBinding selectMainFlowBindingForConfig(Long tenantId, String objectCode, String... fallbackCodes) {
        return runtimeMetadata().selectMainFlowBindingForConfig(tenantId, objectCode, fallbackCodes);
    }

    private BindingLookupResult selectMainFlowBinding(Long tenantId, String objectCode, String... fallbackCodes) {
        return runtimeMetadata().selectMainFlowBinding(tenantId, objectCode, fallbackCodes);
    }

    private String resolveCanonicalObjectCode(Long tenantId, String objectCodeOrConfigKey) {
        return runtimeMetadata().resolveCanonicalObjectCode(tenantId, objectCodeOrConfigKey);
    }

    private BusinessRuntimeContext resolveBusinessRuntimeContext(Long tenantId, String objectCodeOrConfigKey) {
        return runtimeMetadata().resolveBusinessRuntimeContext(tenantId, objectCodeOrConfigKey);
    }

    private BusinessRuntimeContext resolveBusinessRuntimeContext(Long tenantId, String objectCodeOrConfigKey,
                                                                 boolean allowDraftRuntime) {
        return runtimeMetadata().resolveBusinessRuntimeContext(tenantId, objectCodeOrConfigKey, allowDraftRuntime);
    }

    private boolean isBindingEnabled(AiBusinessBinding binding) {
        return BusinessFlowRuntimeMetadata.isBindingEnabled(binding);
    }

    private BusinessFlowRuntimeVO toRuntimeVO(AiBusinessFlowInstanceLink link, String message) {
        return BusinessFlowStatusReader.toRuntimeVO(link, message);
    }

    private String buildBusinessKey(String objectCode, Long recordId) {
        return objectCode + ":" + recordId;
    }

    private BusinessKeyParts parseBusinessKey(String businessKey) {
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":")) {
            throw new BusinessException("业务Key格式错误，应为 objectCode:recordId");
        }
        String[] parts = businessKey.split(":", 2);
        if (StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1])) {
            throw new BusinessException("业务Key格式错误，应为 objectCode:recordId");
        }
        try {
            return new BusinessKeyParts(parts[0], Long.valueOf(parts[1]));
        } catch (NumberFormatException e) {
            throw new BusinessException("业务Key中的记录ID必须是数字");
        }
    }

    /**
     * 查询流程状态
     */
    public JSONObject getFlowStatus(String businessKey) {
        return statusReader().getFlowStatus(businessKey);
    }

    /**
     * 构建流程变量
     */
    private Map<String, Object> buildFlowVariables(JSONObject bindingConfig, Map<String, Object> recordData) {
        return BusinessFlowStartCoordinator.buildFlowVariables(bindingConfig, recordData);
    }

    /**
     * 构建流程标题
     */
    private String buildFlowTitle(JSONObject bindingConfig, Map<String, Object> recordData, String objectCode) {
        return BusinessFlowStartCoordinator.buildFlowTitle(bindingConfig, recordData, objectCode);
    }

    private String resolveBusinessSummary(BusinessObjectVO object,
                                          TaskFormRuntimeContext runtime,
                                          Map<String, Object> recordData) {
        return BusinessFlowDisplayText.resolveBusinessSummary(object, runtime.bindingConfig(), recordData);
    }

    private String replaceTemplateValue(String template, String key, Object value) {
        return BusinessFlowDisplayText.replaceTemplateValue(template, key, value);
    }

    private BusinessFlowBindingVO toVO(String objectCode, AiBusinessBinding binding) {
        JSONObject config = readBindingConfig(binding.getBindingConfig());
        ensureBusinessBinding(config, binding.getTenantId(), objectCode);
        return bindingPresentation.toVO(objectCode, binding, config);
    }

    private void ensureBusinessBinding(JSONObject config, Long tenantId, String objectCode) {
        runtimeMetadata().ensureBusinessBinding(config, tenantId, objectCode);
    }

    private List<Map<String, Object>> collectBusinessFormFieldCatalog(JSONObject schema) {
        return FormAssetFields.collectBusinessFormFieldCatalog(schema);
    }

    private JSONObject readJsonObject(String json) {
        return BusinessFlowRuntimeMetadata.readJsonObject(json);
    }

    private JSONObject readNestedObject(Object value) {
        if (value == null) {
            return new JSONObject();
        }
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (value instanceof Map<?, ?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                return StringUtils.isBlank(text) ? new JSONObject() : JSON.parseObject(text);
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    private JSONArray readNestedArray(Object value) {
        if (value == null) {
            return new JSONArray();
        }
        if (value instanceof JSONArray jsonArray) {
            return jsonArray;
        }
        if (value instanceof List<?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                return StringUtils.isBlank(text) ? new JSONArray() : JSON.parseArray(text);
            } catch (Exception e) {
                return new JSONArray();
            }
        }
        return new JSONArray();
    }

    private void putIfText(Map<String, Object> target, String key, Object value) {
        String text = StringUtils.trimToNull(value == null ? null : String.valueOf(value));
        if (text != null && !"null".equalsIgnoreCase(text)) {
            target.put(key, text);
        }
    }

    private JSONObject readBindingConfig(String bindingConfig) {
        return BusinessFlowRuntimeMetadata.readJsonObject(bindingConfig);
    }

    private String resolveBindingName(JSONObject config) {
        return StringUtils.defaultIfBlank(config.getString("flowModelName"), config.getString("flowModelKey") + " 流程");
    }

    private List<Map<String, Object>> normalizeNodeForms(List<Map<String, Object>> nodeForms) {
        return nodeFormConfiguration.normalizeNodeForms(nodeForms);
    }

    private String normalizeNodeFormMode(String formMode) {
        return nodeFormConfiguration.normalizeNodeFormMode(formMode);
    }

    private String normalizeNodeEditMode(String editMode) {
        return nodeFormConfiguration.normalizeNodeEditMode(editMode);
    }

    private List<Map<String, Object>> normalizeFieldPermissions(Object permissions) {
        return nodeFormConfiguration.normalizeFieldPermissions(permissions);
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? null : text;
    }

    private boolean readBooleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    private Object readRecordValue(Map<String, Object> recordData, String field) {
        return BusinessFlowDisplayText.readRecordValue(recordData, field);
    }

    private String snakeToCamel(String value) {
        return BusinessFlowDisplayText.snakeToCamel(value);
    }

    private String camelToSnake(String value) {
        return BusinessFlowDisplayText.camelToSnake(value);
    }

    private Long resolveTenantId() {
        return BusinessFlowRuntimeMetadata.resolveTenantId();
    }

    private Long resolveUserId() {
        return BusinessFlowRuntimeMetadata.resolveUserId();
    }

    private Long resolveActiveOrgId() {
        return BusinessFlowRuntimeMetadata.resolveActiveOrgId();
    }

    private String resolveUsername() {
        return BusinessFlowRuntimeMetadata.resolveUsername();
    }

    private record BusinessKeyParts(String objectCode, Long recordId) {
    }

    private record TaskFormRuntimeContext(String objectCode,
                                          Long recordId,
                                          String businessKey,
                                          String configKey,
                                          JSONObject bindingConfig) {
    }
}
