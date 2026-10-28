package com.mdframe.forge.lowcode.runtime.service;

import com.mdframe.forge.lowcode.api.config.RuntimeCrudConfigSource;
import com.mdframe.forge.lowcode.api.formula.RuntimeStoredAggregateRefresh;
import com.mdframe.forge.lowcode.formula.runtime.port.StoredFormulaCalculation;
import com.mdframe.forge.lowcode.formula.runtime.port.VirtualFormulaCalculation;
import com.mdframe.forge.lowcode.api.code.BusinessCodeGenerator;
import com.mdframe.forge.lowcode.api.code.RuntimeDocumentNumbers;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.SelectionIdentifierValidator;
import com.mdframe.forge.lowcode.runtime.service.identity.RuntimeIdentityFields;
import com.mdframe.forge.lowcode.runtime.service.identity.RuntimeIdentityReferenceService;
import com.mdframe.forge.lowcode.runtime.service.task.RuntimeTaskLinkFields;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;

import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeFormWriteFields;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeInternalWriteFields;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeCommandFields;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeCommandConditions;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeUniqueValidator;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeUniqueRules;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeUniqueFields;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimePrivacyWrite;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeAutoGeneration;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeWriteFieldMapping;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeEditFieldReader;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeGenerationRules;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimePageQueries;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeRecordQueries;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeExportQueries;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeQueryPlans;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeJoinContext;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeJoinPlanner;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeJoinQuery;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeSearchFields;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeSearchConditions;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeSearchTreeSource;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeDescendantValues;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import com.mdframe.forge.plugin.generator.service.runtime.read.RuntimeReadPipeline;
import com.mdframe.forge.plugin.generator.service.runtime.read.RuntimeFieldAliases;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeMoneyValues;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeStructuredValues;
import com.mdframe.forge.plugin.generator.service.runtime.read.RuntimePrivacyProjection;
import com.mdframe.forge.plugin.generator.service.runtime.read.RuntimeTranslationConfig;
import com.mdframe.forge.plugin.generator.service.runtime.read.RuntimeTranslationProjection;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeStoredFormulaValues;
import com.mdframe.forge.plugin.generator.service.runtime.tree.RuntimeTreeConfigResolver;
import com.mdframe.forge.plugin.generator.service.runtime.tree.RuntimeTreeReader;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.plugin.generator.service.runtime.child.RuntimeFieldRef;
import com.mdframe.forge.plugin.generator.service.runtime.child.RuntimeChildRelation;
import com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy;
import com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowReader;
import com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowWriter;
import com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowOwnership;
import com.mdframe.forge.plugin.generator.service.runtime.child.ChildRelationResolver;
import com.mdframe.forge.plugin.generator.service.runtime.child.ChildDetailRelationResolver;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.validatedChildrenPayload;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.hasWritableChildData;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.isImmutableWriteField;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.IMMUTABLE_WRITE_FIELDS;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeWriteTransactions;
import java.util.function.Supplier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态CRUD服务
 * 基于DynamicCrudRepository实现，支持配置驱动的通用CRUD操作
 *
 * @author forge
 */
@Slf4j(topic = "com.mdframe.forge.plugin.generator.service.DynamicCrudService")
public class RuntimeCrudService {

    private static final String MASTER_DETAIL_LAYOUT = "master-detail-crud";

    private final RuntimePageQueries pageQueries;
    private final RuntimeRecordQueries recordQueries;
    private final RuntimeExportQueries exportQueries;
    private final RuntimeFieldMetadata fieldMetadata;
    private final RuntimeReadPipeline readPipeline;
    private final RuntimeMoneyValues moneyValues;
    private final RuntimeStructuredValues structuredValues;
    private final RuntimeTreeConfigResolver treeConfigResolver;
    private final RuntimeTreeReader treeReader;
    private final DynamicCrudRepository repository;
    private final RuntimeCrudConfigSource runtimeConfigs;
    private final ObjectMapper objectMapper;
    private final RuntimePrivacyWrite privacyWrite;
    private final RuntimeDataScopeService dynamicDataScopeService;
    private final RuntimeAutoGeneration autoGeneration;
    private final RuntimeWriteFieldMapping writeFieldMapping;
    private final RuntimeUniqueValidator uniqueValidator;
    private final RuntimeCommandFields commandFields;
    private final RuntimeFormWriteFields formWriteFields;
    private final RuntimeInternalWriteFields internalWriteFields;
    private final RuntimeCommandConditions commandConditions;
    private final RuntimeStoredAggregateRefresh storedAggregateRefreshService;
    private final RuntimeStoredFormulaValues storedFormulaValues;
    private final RuntimeDataSourceResolution runtimeDataSourceResolver;

    private RuntimeWriteTransactions runtimeWriteTransactions;
    private RuntimeIdentityReferenceService identityReferences;

    @Autowired
    public void setIdentityReferences(RuntimeIdentityReferenceService identityReferences) {
        this.identityReferences = identityReferences;
    }

    @Autowired
    public void setRuntimeWriteTransactions(RuntimeWriteTransactions runtimeWriteTransactions) {
        this.runtimeWriteTransactions = runtimeWriteTransactions;
    }

    private final RuntimeJoinPlanner joinPlanner;
    private final RuntimeJoinQuery joinQuery;
    private final ChildRowReader childRowReader;
    private final ChildRowWriter childRowWriter;

    public RuntimeCrudService(DynamicCrudRepository repository,
                              RuntimeCrudConfigSource runtimeConfigs,
                              ObjectMapper objectMapper,
                              DictValueProvider dictValueProvider,
                              DesensitizeStrategyFactory desensitizeStrategyFactory,
                              PersistentCryptoService persistentCryptoService,
                              LowcodeEncryptConfigParser encryptConfigParser,
                              RuntimeDataScopeService dynamicDataScopeService,
                              RuntimeDocumentNumbers documentNumbers,
                              BusinessCodeGenerator codeRuleService,
                              RuntimeStoredAggregateRefresh storedAggregateRefreshService,
                              StoredFormulaCalculation storedFormulaRuntime,
                              VirtualFormulaCalculation virtualFormulaRuntime,
                              RuntimeDataSourceResolution runtimeDataSourceResolver) {
        this.repository = repository;
        this.fieldMetadata = new RuntimeFieldMetadata(objectMapper);
        this.moneyValues = new RuntimeMoneyValues(fieldMetadata);
        this.structuredValues = new RuntimeStructuredValues(objectMapper, fieldMetadata);
        this.readPipeline = new RuntimeReadPipeline(fieldMetadata,
                new RuntimeFieldAliases(fieldMetadata), moneyValues, structuredValues,
                new RuntimePrivacyProjection(objectMapper, persistentCryptoService, encryptConfigParser, desensitizeStrategyFactory),
                virtualFormulaRuntime, new RuntimeTranslationConfig(objectMapper),
                new RuntimeTranslationProjection(objectMapper, dictValueProvider));

        this.treeConfigResolver = new RuntimeTreeConfigResolver(objectMapper);
        RuntimeSearchTreeSource searchTrees = new RuntimeSearchTreeSource(objectMapper, treeConfigResolver);
        RuntimeSearchFields searchFields = new RuntimeSearchFields(objectMapper, searchTrees);
        RuntimeSearchConditions searchConditions = new RuntimeSearchConditions(new RuntimeDescendantValues(repository, searchTrees));
        this.treeReader = new RuntimeTreeReader(repository, dynamicDataScopeService);
        this.runtimeConfigs = runtimeConfigs;
        this.objectMapper = objectMapper;
        this.dynamicDataScopeService = dynamicDataScopeService;
        this.writeFieldMapping = new RuntimeWriteFieldMapping(repository, fieldMetadata);
        this.privacyWrite = new RuntimePrivacyWrite(objectMapper, persistentCryptoService,
                encryptConfigParser, writeFieldMapping);
        this.commandFields = new RuntimeCommandFields(repository, objectMapper, fieldMetadata, writeFieldMapping);
        this.formWriteFields = new RuntimeFormWriteFields(repository, objectMapper, fieldMetadata);
        this.internalWriteFields = new RuntimeInternalWriteFields(repository, objectMapper, fieldMetadata, writeFieldMapping);
        this.commandConditions = new RuntimeCommandConditions(commandFields, privacyWrite);
        RuntimeEditFieldReader editFields = new RuntimeEditFieldReader(objectMapper);
        this.uniqueValidator = new RuntimeUniqueValidator(repository, fieldMetadata,
                new RuntimeUniqueRules(editFields), new RuntimeUniqueFields(editFields, writeFieldMapping));
        this.autoGeneration = new RuntimeAutoGeneration(
                new RuntimeGenerationRules(fieldMetadata, editFields), writeFieldMapping, codeRuleService,
                documentNumbers);
        this.storedAggregateRefreshService = storedAggregateRefreshService;
        this.storedFormulaValues = new RuntimeStoredFormulaValues(repository, fieldMetadata, storedFormulaRuntime);
        this.runtimeDataSourceResolver = runtimeDataSourceResolver;
        this.joinPlanner = new RuntimeJoinPlanner(repository, objectMapper, fieldMetadata,
                writeFieldMapping, new ChildRelationResolver(repository));
        this.joinQuery = new RuntimeJoinQuery(objectMapper);
        RuntimeQueryPlans queryPlans = new RuntimeQueryPlans(writeFieldMapping, searchFields, searchConditions, joinPlanner);
        this.pageQueries = new RuntimePageQueries(repository, queryPlans, joinQuery, objectMapper,
                dynamicDataScopeService, readPipeline);
        this.exportQueries = new RuntimeExportQueries(repository, queryPlans, joinQuery, objectMapper,
                dynamicDataScopeService, readPipeline);
        this.childRowReader = new ChildRowReader(repository, new ChildDetailRelationResolver(repository, objectMapper));
        this.recordQueries = new RuntimeRecordQueries(repository, joinPlanner, dynamicDataScopeService, readPipeline, childRowReader);
        this.childRowWriter = new ChildRowWriter(repository, new ChildRowOwnership(repository));
    }

    // ==================== 查询操作 ====================

    /**
     * 分页查询
     */
    public Page<Map<String, Object>> selectPage(String configKey, PageQuery pageQuery, DynamicCrudQuery query) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return pageQueries.page(config, pageQuery, query, currentPrimaryKey());
        }
    }

    /**
     * 查询动态导出数据，复用动态 CRUD 的字段白名单、解密、字典翻译和脱敏链路。
     */
    public List<Map<String, Object>> selectExportRows(String configKey,
                                                      DynamicCrudQuery query,
                                                      Integer maxRows) {
        return selectExportPageRows(configKey, query, 1, RuntimeExportQueries.limit(maxRows), null);
    }

    /**
     * 定时触发器候选记录读取。只允许按运行配置字段白名单内的到期字段做区间查询，
     * 避免后台扫描器出现无条件全表读取。
     */
    public List<Map<String, Object>> selectScheduledCandidateRows(String configKey,
                                                                  String dueField,
                                                                  LocalDateTime windowStart,
                                                                  LocalDateTime windowEnd,
                                                                  Integer batchSize) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return pageQueries.scheduled(config, dueField, windowStart, windowEnd, batchSize, currentPrimaryKey());
        }
    }

    /**
     * 统计动态导出数据量，供同步/异步导出决策使用。
     */
    public long countExportRows(String configKey,
                                DynamicCrudQuery query,
                                DataScopeContext dataScopeContext) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return exportQueries.count(config, query, dataScopeContext, currentPrimaryKey());
        }
    }

    /**
     * 分页查询动态导出数据，不重复统计 count，供异步导出分批写入使用。
     */
    public List<Map<String, Object>> selectExportPageRows(String configKey,
                                                          DynamicCrudQuery query,
                                                          Integer pageNum,
                                                          Integer pageSize,
                                                          DataScopeContext dataScopeContext) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return exportQueries.page(config, query, pageNum, pageSize, dataScopeContext, currentPrimaryKey());
        }
    }

    /**
     * 查询树形导航数据，供树形单表模板左侧树使用。
     */
    public List<Map<String, Object>> selectTree(String configKey) {
        return selectTree(configKey, null, null, null);
    }

    /**
     * 查询树形数据。loadMode=full 返回完整树，loadMode=lazy 按 parentValue 返回一层子节点。
     */
    public List<Map<String, Object>> selectTree(String configKey, String parentValue, String parentId, String loadMode) {
        return selectTree(configKey, parentValue, parentId, loadMode, null, null);
    }

    /**
     * 查询树形数据。loadMode=full 返回完整树，loadMode=lazy 按 parentValue 返回一层子节点。
     */
    public List<Map<String, Object>> selectTree(String configKey,
                                                String parentValue,
                                                String parentId,
                                                String loadMode,
                                                String orderByColumn,
                                                String isAsc) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            LowcodeTreeConfig treeConfig = treeConfigResolver.resolve(config);
            String tableName = StringUtils.defaultIfBlank(treeConfig.getSourceTableName(), config.getTableName());
            RuntimeTreeReader.Rows rows = treeReader.read(config, tableName, treeConfig,
                    buildRuntimeColumnMapping(config, tableName),
                    new RuntimeTreeReader.Request(parentValue, parentId, loadMode, orderByColumn, isAsc));
            applyReadPipeline(rows.values(), config);
            return treeReader.nodes(rows);
        }
    }

    /**
     * 自定义分页查询。
     */
    public Page<Map<String, Object>> selectCustomPage(String configKey, CustomQueryExecuteDTO request) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return pageQueries.custom(config, request, currentPrimaryKey());
        }
    }

    /**
     * 根据ID查询
     */
    public Map<String, Object> selectById(String configKey, Object id) {
        return readRecordByConfig(getConfig(configKey), id);
    }

    /**
     * 业务流程审批节点读取业务记录：优先已发布配置，工作台草稿对象也允许按当前配置读取。
     */
    public Map<String, Object> selectByIdAllowDraft(String configKey, Object id) {
        LowcodeRuntimeConfig config = runtimeConfigs.loadAllowDraft(configKey);
        return readRecordByConfig(config, id);
    }

    private Map<String, Object> readRecordByConfig(LowcodeRuntimeConfig config, Object id) {
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return recordQueries.byId(config, id, currentPrimaryKey(), isMasterDetailRuntime(config));
        }
    }

    /**
     * 按运行时主键批量读取记录，供运行态批量能力复用动态 CRUD 的数据源、数据权限和读取后处理链路。
     */
    public Map<Object, Map<String, Object>> selectByIds(String configKey, Collection<?> ids) {
        List<Object> normalizedIds = recordQueries.normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        LowcodeRuntimeConfig config = getConfig(configKey);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return recordQueries.byIds(config, normalizedIds, currentPrimaryKey());
        }
    }

    // ==================== 新增操作 ====================

    /**
     * 新增
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> insert(String configKey, Map<String, Object> data) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        assertRuntimeWritable(config);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return executeRuntimeWrite(() -> {
                String tableName = config.getTableName();

                // 获取字段映射
                Map<String, String> columnMapping = buildRuntimeColumnMapping(config, tableName);

                // 获取允许写入的字段。editSchema 是运行态表单白名单，modelSchema 兜底承接保存设计器后新增但尚未重建 editSchema 的字段。
                Set<String> allowedFields = buildAllowedWriteFields(config, tableName);
                autoGeneration.apply(config, data == null ? null : extractMainPayload(data), allowedFields);
                autoGeneration.applyDocumentNumber(config, data == null ? null : extractMainPayload(data), allowedFields);
                SelectionIdentifierValidator.validateRuntime(config, data, objectMapper);
                validatePublicReferences(config, data, null);

                RuntimeJoinContext joinContext = joinPlanner.plan(config);
                if (isMasterDetailRuntime(config) && joinContext != null) {
                    insertMasterDetailData(config, data, allowedFields, joinContext);
                    return data;
                }
                if (joinContext != null) {
                    insertJoinedData(config, data, allowedFields, joinContext);
                    return data;
                }

                // 过滤前先计算 STORED 公式，确保公式结果参与写库。
                storedFormulaValues.apply(config, data);
                Map<String, Object> filteredData = formWriteFields.columns(data, allowedFields, columnMapping, null);
                applyAssignedPrimaryKey(data, filteredData);

                if (filteredData.isEmpty()) {
                    throw new BusinessException("没有可写入的字段");
                }

                validateUniqueConstraints(config, tableName, data, null, null);

                // 应用加密
                moneyValues.store(filteredData, config);
                structuredValues.store(filteredData, config);
                privacyWrite.encrypt(filteredData, config.getEncryptConfig());

                // 执行插入
                LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
                Object id = repository.insertReturningKey(
                        tableName,
                        filteredData,
                        primaryKeyColumn(primaryKey),
                        primaryKeyAutoIncrement(primaryKey));
                if (id == null) {
                    storedAggregateRefreshService.refreshRuntimeAfterChildInsert(config, data);
                    return data;
                }
                Map<String, Object> result = selectById(configKey, id);
                if (result != null) {
                    storedAggregateRefreshService.refreshRuntimeAfterChildInsert(config, result);
                    return result;
                }
                Map<String, Object> fallback = new LinkedHashMap<>(data);
                putPrimaryKeyAlias(fallback, primaryKey, id);
                storedAggregateRefreshService.refreshRuntimeAfterChildInsert(config, fallback);
                return fallback;
            });
        }
    }

    /**
     * 内部运行态创建记录。用于触发器创建关联记录，校验发布态配置和模型字段，
     * 不依赖前端编辑表单是否展示该字段。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> insertInternal(String configKey, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            throw new BusinessException("没有可写入的字段");
        }
        LowcodeRuntimeConfig config = getConfig(configKey);
        assertRuntimeWritable(config);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return executeRuntimeWrite(() -> {
                String tableName = config.getTableName();
                Set<String> allowedFields = internalWriteFields.allowed(config, tableName);
                autoGeneration.apply(config, data == null ? null : extractMainPayload(data), allowedFields);
                autoGeneration.applyDocumentNumber(config, data == null ? null : extractMainPayload(data), allowedFields);
                storedFormulaValues.apply(config, data);
                Map<String, Object> filteredData = internalWriteFields.modelColumns(config, tableName, data);
                applyAssignedPrimaryKey(data, filteredData);
                if (filteredData.isEmpty()) {
                    throw new BusinessException("没有可写入的字段");
                }
                validateUniqueConstraints(config, tableName, data, null, null);
                structuredValues.store(filteredData, config);
                privacyWrite.encrypt(filteredData, config.getEncryptConfig());
                LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
                Object id = repository.insertReturningKey(
                        tableName,
                        filteredData,
                        primaryKeyColumn(primaryKey),
                        primaryKeyAutoIncrement(primaryKey));
                Map<String, Object> result = id == null ? null : selectById(configKey, id);
                if (result != null) {
                    storedAggregateRefreshService.refreshRuntimeAfterChildInsert(config, result);
                    return result;
                }
                Map<String, Object> fallback = new LinkedHashMap<>(data);
                putPrimaryKeyAlias(fallback, primaryKey, id);
                storedAggregateRefreshService.refreshRuntimeAfterChildInsert(config, fallback);
                return fallback;
            });
        }
    }

    /**
     * 事务型业务命令创建记录。字段只允许来自已发布模型/页面协议，不使用数据库全列兜底。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> insertCommandRecord(String configKey, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            throw new BusinessException("没有可写入的字段");
        }
        LowcodeRuntimeConfig config = getConfig(configKey);
        assertRuntimeWritable(config);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return executeRuntimeWrite(() -> {
                String tableName = config.getTableName();
                Set<String> allowedFields = commandFields.published(config);
                autoGeneration.apply(config, data == null ? null : extractMainPayload(data), allowedFields);
                autoGeneration.applyDocumentNumber(config, data == null ? null : extractMainPayload(data), allowedFields);
                storedFormulaValues.apply(config, data);
                Map<String, Object> filteredData = commandFields.write(config, tableName, data);
                applyAssignedPrimaryKey(data, filteredData);
                if (filteredData.isEmpty()) {
                    throw new BusinessException("没有可写入的字段");
                }
                validateUniqueConstraints(config, tableName, data, null, null);
                structuredValues.store(filteredData, config);
                privacyWrite.encrypt(filteredData, config.getEncryptConfig());
                LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
                Object id = repository.insertReturningKey(
                        tableName, filteredData, primaryKeyColumn(primaryKey), primaryKeyAutoIncrement(primaryKey));
                Map<String, Object> result = id == null ? null : selectById(configKey, id);
                if (result != null) {
                    storedAggregateRefreshService.refreshRuntimeAfterChildInsert(config, result);
                    return result;
                }
                Map<String, Object> fallback = new LinkedHashMap<>(data);
                putPrimaryKeyAlias(fallback, primaryKey, id);
                storedAggregateRefreshService.refreshRuntimeAfterChildInsert(config, fallback);
                return fallback;
            });
        }
    }

    // ==================== 更新操作 ====================

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateById(String configKey, Map<String, Object> data) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        assertRuntimeWritable(config);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            executeRuntimeWrite(() -> {
                String tableName = config.getTableName();

                // 获取ID
                LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
                Object idValue = resolvePayloadId(data, primaryKey);
                if (idValue == null) {
                    throw new BusinessException("更新操作缺少id");
                }
                Object id = idValue;

                // 获取字段映射
                Map<String, String> columnMapping = buildRuntimeColumnMapping(config, tableName);

                // 获取允许写入的字段。editSchema 是运行态表单白名单，modelSchema 兜底承接保存设计器后新增但尚未重建 editSchema 的字段。
                Set<String> allowedFields = buildAllowedWriteFields(config, tableName);
                SelectionIdentifierValidator.validateRuntime(config, data, objectMapper);
                validatePublicReferences(config, data, id);

                RuntimeJoinContext joinContext = joinPlanner.plan(config);
                if (isMasterDetailRuntime(config) && joinContext != null) {
                    updateMasterDetailData(config, id, data, allowedFields, joinContext);
                    return null;
                }
                if (joinContext != null) {
                    updateJoinedData(config, id, data, allowedFields, joinContext);
                    return null;
                }

                // 公式字段需要基于完整旧值上下文计算，确保部分更新不会把公式算空。
                DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, tableName, null);
                Map<String, Object> beforeRecord = storedFormulaValues.applyUpdate(config, tableName, id, data, dataScopeCondition, currentPrimaryKey());

                // 过滤并转换字段名
                Map<String, Object> filteredData = formWriteFields.columns(data, allowedFields, columnMapping, primaryKey);
                privacyWrite.removeMaskedColumns(filteredData, config, tableName);

                if (filteredData.isEmpty()) {
                    throw new BusinessException("没有可更新的字段");
                }

                validateUniqueConstraints(config, tableName, data, beforeRecord, id);

                // 应用加密
                moneyValues.store(filteredData, config);
                structuredValues.store(filteredData, config);
                privacyWrite.encrypt(filteredData, config.getEncryptConfig());

                // 执行更新
                int affected = repository.updateById(tableName, primaryKeyColumn(primaryKey), id, filteredData, dataScopeCondition);
                if (affected <= 0) {
                    throw new BusinessException("无权限更新该数据或数据不存在");
                }
                storedAggregateRefreshService.refreshRuntimeAfterChildUpdate(config, beforeRecord,
                        repository.selectById(tableName, primaryKeyColumn(primaryKey), id, null));
                return null;
            });
        }
    }

    /**
     * 内部运行态字段更新。用于单据状态等系统驱动字段，不受编辑表单 schema 限制，
     * 但仍校验动态表真实列名、租户条件和数据权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateInternalFieldsById(String configKey, Object id, Map<String, Object> data) {
        updateInternalFieldsByConfig(getConfig(configKey), id, data);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateInternalFieldsByIdAllowDraft(String configKey, Object id, Map<String, Object> data) {
        LowcodeRuntimeConfig config = runtimeConfigs.loadAllowDraft(configKey);
        updateInternalFieldsByConfig(config, id, data);
    }

    private void updateInternalFieldsByConfig(LowcodeRuntimeConfig config, Object id, Map<String, Object> data) {
        if (id == null) {
            throw new BusinessException("更新操作缺少id");
        }
        if (data == null || data.isEmpty()) {
            throw new BusinessException("没有可更新的字段");
        }
        assertRuntimeWritable(config);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            executeRuntimeWrite(() -> {
                String tableName = config.getTableName();
                LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
                RuntimeInternalWriteFields.ColumnMetadata columns = internalWriteFields.columns(config, tableName);
                DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, tableName, null);
                Map<String, Object> beforeRecord = storedFormulaValues.applyUpdate(config, tableName, id, data, dataScopeCondition, currentPrimaryKey());

                Map<String, Object> filteredData = internalWriteFields.physicalColumns(
                        config, tableName, data, columns, primaryKey);

                if (filteredData.isEmpty()) {
                    throw new BusinessException("没有可更新的字段");
                }

                structuredValues.store(filteredData, config);
                privacyWrite.encrypt(filteredData, config.getEncryptConfig());
                int affected = repository.updateById(tableName, primaryKeyColumn(primaryKey), id, filteredData, dataScopeCondition);
                if (affected <= 0) {
                    throw new BusinessException("无权限更新该数据或数据不存在");
                }
                storedAggregateRefreshService.refreshRuntimeAfterChildUpdate(config, beforeRecord,
                        repository.selectById(tableName, primaryKeyColumn(primaryKey), id, null));
                return null;
            });
        }
    }

    /**
     * 内部运行态字段更新。用于触发器更新字段，字段必须存在于发布态模型或动态表。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFieldsInternal(String configKey, Object id, Map<String, Object> fields) {
        if (id == null) {
            throw new BusinessException("更新操作缺少id");
        }
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("没有可更新的字段");
        }
        LowcodeRuntimeConfig config = getConfig(configKey);
        assertRuntimeWritable(config);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            executeRuntimeWrite(() -> {
                String tableName = config.getTableName();
                LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
                DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, tableName, null);
                Map<String, Object> beforeRecord = storedFormulaValues.applyUpdate(config, tableName, id, fields, dataScopeCondition, currentPrimaryKey());
                Map<String, Object> filteredData = internalWriteFields.modelColumns(config, tableName, fields);
                if (filteredData.isEmpty()) {
                    throw new BusinessException("没有可更新的字段");
                }
                structuredValues.store(filteredData, config);
                privacyWrite.encrypt(filteredData, config.getEncryptConfig());
                removePrimaryKeyColumns(filteredData, primaryKey);
                int affected = repository.updateById(tableName, primaryKeyColumn(primaryKey), id, filteredData, dataScopeCondition);
                if (affected <= 0) {
                    throw new BusinessException("无权限更新该数据或数据不存在");
                }
                storedAggregateRefreshService.refreshRuntimeAfterChildUpdate(config, beforeRecord,
                        repository.selectById(tableName, primaryKeyColumn(primaryKey), id, null));
                return null;
            });
        }
    }

    /**
     * 事务型命令条件更新。期望值、数据权限、租户和逻辑删除条件与写入处于同一 UPDATE。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCommandFields(String configKey,
                                    Object id,
                                    Map<String, Object> fields,
                                    Map<String, Object> expectedFields) {
        if (id == null) {
            throw new BusinessException("更新操作缺少id");
        }
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("没有可更新的字段");
        }
        LowcodeRuntimeConfig config = getConfig(configKey);
        assertRuntimeWritable(config);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            executeRuntimeWrite(() -> {
                String tableName = config.getTableName();
                LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
                DynamicCrudRepository.SqlCondition dataScope = buildDataScopeCondition(config, tableName, null);
                Map<String, Object> beforeRecord = storedFormulaValues.applyUpdate(
                        config, tableName, id, fields, dataScope, currentPrimaryKey());
                Map<String, Object> filteredData = commandFields.write(config, tableName, fields);
                if (filteredData.isEmpty()) {
                    throw new BusinessException("没有可更新的字段");
                }
                structuredValues.store(filteredData, config);
                privacyWrite.encrypt(filteredData, config.getEncryptConfig());
                removePrimaryKeyColumns(filteredData, primaryKey);
                DynamicCrudRepository.SqlCondition expected = commandConditions.expected(
                        config, tableName, expectedFields);
                int affected = repository.updateById(
                        tableName, primaryKeyColumn(primaryKey), id, filteredData,
                        commandConditions.combine(dataScope, expected));
                if (affected <= 0) {
                    throw new BusinessException("记录已变化、数值越界或无权限更新");
                }
                storedAggregateRefreshService.refreshRuntimeAfterChildUpdate(config, beforeRecord,
                        repository.selectById(tableName, primaryKeyColumn(primaryKey), id, null));
                return null;
            });
        }
    }

    /**
     * 事务型命令多字段数值原子调整。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustCommandNumbers(String configKey,
                                     Object id,
                                     Map<String, BigDecimal> deltas,
                                     Map<String, BigDecimal> minimums,
                                     Map<String, BigDecimal> maximums,
                                     Map<String, Object> expectedFields) {
        if (id == null) {
            throw new BusinessException("数值调整缺少目标记录 ID");
        }
        if (deltas == null || deltas.isEmpty()) {
            throw new BusinessException("数值调整字段不能为空");
        }
        LowcodeRuntimeConfig config = getConfig(configKey);
        assertRuntimeWritable(config);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            executeRuntimeWrite(() -> {
                String tableName = config.getTableName();
                LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
                Map<String, BigDecimal> mappedDeltas = commandFields.decimals(config, tableName, deltas);
                Map<String, BigDecimal> mappedMinimums = commandFields.decimals(config, tableName, minimums);
                Map<String, BigDecimal> mappedMaximums = commandFields.decimals(config, tableName, maximums);
                DynamicCrudRepository.SqlCondition dataScope = buildDataScopeCondition(config, tableName, null);
                DynamicCrudRepository.SqlCondition expected = commandConditions.expected(
                        config, tableName, expectedFields);
                Map<String, Object> before = repository.selectById(
                        tableName, primaryKeyColumn(primaryKey), id, dataScope);
                if (before == null) {
                    throw new BusinessException("记录不存在或无权限调整");
                }
                int affected = repository.adjustNumbersById(
                        tableName, primaryKeyColumn(primaryKey), id,
                        mappedDeltas, mappedMinimums, mappedMaximums,
                        commandConditions.combine(dataScope, expected));
                if (affected <= 0) {
                    throw new BusinessException("记录已变化、数值越界或无权限调整");
                }
                storedAggregateRefreshService.refreshRuntimeAfterChildUpdate(config, before,
                        repository.selectById(tableName, primaryKeyColumn(primaryKey), id, null));
                return null;
            });
        }
    }

    /**
     * 在当前本地事务中锁定目标记录并校验发布态 expected 字段。
     * 用于状态门禁，避免先查后改产生竞态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assertCommandRecord(String configKey, Object id, Map<String, Object> expectedFields) {
        assertCommandRecord(configKey, id, expectedFields, List.of());
    }

    /**
     * 在行锁门禁中增加受控的字段间数值比较，例如 pickedQuantity >= form.quantity。
     * 比较字段和值均已由动作协议校验并映射到发布模型字段，禁止传入 SQL 或任意列名。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assertCommandRecord(String configKey,
                                    Object id,
                                    Map<String, Object> expectedFields,
                                    List<Map<String, Object>> numericConstraints) {
        if (id == null || StringUtils.isBlank(String.valueOf(id))) {
            throw new BusinessException("记录门禁缺少目标记录 ID");
        }
        LowcodeRuntimeConfig config = getConfig(configKey);
        assertRuntimeWritable(config);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            executeRuntimeWrite(() -> {
                DynamicCrudRepository.SqlCondition dataScope = buildDataScopeCondition(config, config.getTableName(), null);
                DynamicCrudRepository.SqlCondition expected = commandConditions.expected(
                        config, config.getTableName(), expectedFields);
                DynamicCrudRepository.SqlCondition numeric = commandConditions.numeric(
                        config, config.getTableName(), numericConstraints);
                Map<String, Object> record = repository.selectByIdForUpdate(
                        config.getTableName(), primaryKeyColumn(currentPrimaryKey()), id,
                        commandConditions.combine(commandConditions.combine(dataScope, expected), numeric));
                if (record == null) {
                    throw new BusinessException("记录状态不满足门禁、无权限访问或记录不存在");
                }
                return null;
            });
        }
    }

    /** 本地事务命令只能使用默认 Forge 主数据源。 */
    public void assertLocalTransactionConfig(String configKey) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        if (config.getRuntimeDatasourceId() != null
                || StringUtils.isNotBlank(config.getRuntimeDatasourceCode())) {
            throw new BusinessException(UiMessages.translate("本地事务动作不支持外接运行数据源: ") + configKey);
        }
    }

    private void insertMasterDetailData(LowcodeRuntimeConfig config,
                                        Map<String, Object> data,
                                        Set<String> allowedFields,
                                        RuntimeJoinContext joinContext) {
        Map<String, Object> childrenPayload = validatedChildrenPayload(data, joinContext.childRelations());
        Map<String, Object> mainPayload = extractMainPayload(data);
        storedFormulaValues.apply(config, mainPayload);
        validateUniqueConstraints(config, config.getTableName(), mainPayload, null, null);
        Map<String, Object> primaryData = formWriteFields.primaryColumns(mainPayload, allowedFields, joinContext);
        applyAssignedPrimaryKey(mainPayload, primaryData);
        if (primaryData.isEmpty()) {
            throw new BusinessException("没有可写入的主表字段");
        }

        moneyValues.store(primaryData, config);
        structuredValues.store(primaryData, config);
        privacyWrite.encrypt(primaryData, config.getEncryptConfig());
        LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
        Object mainId = repository.insertReturningKey(
                config.getTableName(),
                primaryData,
                primaryKeyColumn(primaryKey),
                primaryKeyAutoIncrement(primaryKey));
        putPrimaryKeyAlias(mainPayload, primaryKey, mainId);
        boolean childrenChanged = childRowWriter.insertMasterDetails(
                new ChildRowWriter.ParentRecord(config.getTableName(), mainId, primaryData, null),
                joinContext.childRelations(), childrenPayload);
        if (childrenChanged) {
            refreshRecordById(config, mainId);
        }
    }

    private void updateMasterDetailData(LowcodeRuntimeConfig config,
                                        Object id,
                                        Map<String, Object> data,
                                        Set<String> allowedFields,
                                        RuntimeJoinContext joinContext) {
        Map<String, Object> childrenPayload = validatedChildrenPayload(data, joinContext.childRelations());
        DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, config.getTableName(), null);
        Map<String, Object> authorizedMainRecord = repository.selectById(config.getTableName(), id, dataScopeCondition);
        if (authorizedMainRecord == null) {
            throw new BusinessException("无权限更新该数据或数据不存在");
        }
        Map<String, Object> mainPayload = extractMainPayload(data);
        storedFormulaValues.applyUpdate(config, config.getTableName(), id, mainPayload, dataScopeCondition, authorizedMainRecord, currentPrimaryKey());
        validateUniqueConstraints(config, config.getTableName(), mainPayload, authorizedMainRecord, id);
        Map<String, Object> primaryData = formWriteFields.primaryColumns(mainPayload, allowedFields, joinContext);
        removePrimaryKeyColumns(primaryData, currentPrimaryKey());
        privacyWrite.removeMaskedColumns(primaryData, config, config.getTableName());
        if (!primaryData.isEmpty()) {
            moneyValues.store(primaryData, config);
            structuredValues.store(primaryData, config);
            privacyWrite.encrypt(primaryData, config.getEncryptConfig());
            int affected = repository.updateById(config.getTableName(), id, primaryData, dataScopeCondition);
            if (affected <= 0) {
                throw new BusinessException("无权限更新该数据或数据不存在");
            }
        }

        boolean childrenChanged = childRowWriter.updateMasterDetails(
                new ChildRowWriter.ParentRecord(config.getTableName(), id, primaryData, authorizedMainRecord),
                joinContext.childRelations(), childrenPayload);
        if (primaryData.isEmpty() && !childrenChanged) {
            throw new BusinessException("没有可更新的字段");
        }
        if (childrenChanged) {
            refreshRecordById(config, id);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMainPayload(Map<String, Object> data) {
        if (data != null && data.get("main") instanceof Map<?, ?> main) {
            return (Map<String, Object>) main;
        }
        return data == null ? Map.of() : data;
    }

    @SuppressWarnings("unchecked")
    private Object resolvePayloadId(Map<String, Object> data, LowcodePrimaryKeyStrategy primaryKey) {
        if (data == null) {
            return null;
        }
        Object idValue = firstPresent(data, primaryKeyField(primaryKey), primaryKeyColumn(primaryKey), "id");
        if (idValue != null) {
            return idValue;
        }
        Object main = data.get("main");
        if (main instanceof Map<?, ?> map) {
            Map<String, Object> mainData = (Map<String, Object>) map;
            return firstPresent(mainData, primaryKeyField(primaryKey), primaryKeyColumn(primaryKey), "id");
        }
        return null;
    }

    private void applyAssignedPrimaryKey(Map<String, Object> payload, Map<String, Object> columns) {
        LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
        if (!primaryKeyAutoIncrement(primaryKey)) {
            Object id = resolvePayloadId(payload, primaryKey);
            if (id != null && StringUtils.isNotBlank(String.valueOf(id))) {
                columns.put(primaryKeyColumn(primaryKey), id);
            }
        }
    }

    private LowcodePrimaryKeyStrategy currentPrimaryKey() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        LowcodePrimaryKeyStrategy primaryKey = context == null ? null : context.getPrimaryKey();
        if (primaryKey == null) {
            primaryKey = new LowcodePrimaryKeyStrategy();
            primaryKey.setField("id");
            primaryKey.setColumnName("id");
            primaryKey.setDataType("bigint");
            primaryKey.setAutoIncrement(true);
        }
        return primaryKey;
    }

    private String primaryKeyField(LowcodePrimaryKeyStrategy primaryKey) {
        return StringUtils.defaultIfBlank(primaryKey == null ? null : primaryKey.getField(), "id");
    }

    private String primaryKeyColumn(LowcodePrimaryKeyStrategy primaryKey) {
        return StringUtils.defaultIfBlank(primaryKey == null ? null : primaryKey.getColumnName(), "id");
    }

    private boolean primaryKeyAutoIncrement(LowcodePrimaryKeyStrategy primaryKey) {
        return primaryKey == null || primaryKey.getAutoIncrement() == null || primaryKey.getAutoIncrement();
    }

    private void removePrimaryKeyColumns(Map<String, Object> data, LowcodePrimaryKeyStrategy primaryKey) {
        if (data == null || data.isEmpty()) {
            return;
        }
        data.remove(primaryKeyField(primaryKey));
        data.remove(primaryKeyColumn(primaryKey));
        data.remove(DynamicQueryGenerator.snakeToCamel(primaryKeyColumn(primaryKey)));
        data.remove(DynamicQueryGenerator.camelToSnake(primaryKeyField(primaryKey)));
        data.remove("id");
    }

    private void putPrimaryKeyAlias(Map<String, Object> data, LowcodePrimaryKeyStrategy primaryKey, Object id) {
        if (data == null || id == null) {
            return;
        }
        data.put(primaryKeyField(primaryKey), id);
        data.put(primaryKeyColumn(primaryKey), id);
        data.put("id", id);
    }

    private void refreshRecordById(LowcodeRuntimeConfig config, Object id) {
        if (id != null && StringUtils.isNotBlank(String.valueOf(id))) {
            storedAggregateRefreshService.refreshRuntimeRecord(config, id);
        }
    }

    private Object firstPresent(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (StringUtils.isNotBlank(key) && data.containsKey(key)) {
                return data.get(key);
            }
        }
        return null;
    }

    private void insertJoinedData(LowcodeRuntimeConfig config,
                                  Map<String, Object> data,
                                  Set<String> allowedFields,
                                  RuntimeJoinContext joinContext) {
        storedFormulaValues.apply(config, data);
        validateUniqueConstraints(config, config.getTableName(), data, null, null);
        ChildRowPolicy.WritePartition partition = ChildRowPolicy.partition(
                data, allowedFields, joinContext.fields(), joinContext.childRelations());
        Map<String, Object> primaryData = partition.primaryData();
        applyAssignedPrimaryKey(data, primaryData);
        Map<String, Map<String, Object>> childDataMap = partition.childData();
        if (primaryData.isEmpty()) {
            throw new BusinessException("没有可写入的主表字段");
        }
        moneyValues.store(primaryData, config);
        structuredValues.store(primaryData, config);
        privacyWrite.encrypt(primaryData, config.getEncryptConfig());
        LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
        Object mainId = repository.insertReturningKey(
                config.getTableName(),
                primaryData,
                primaryKeyColumn(primaryKey),
                primaryKeyAutoIncrement(primaryKey));
        putPrimaryKeyAlias(data, primaryKey, mainId);
        boolean childrenChanged = childRowWriter.insertJoined(
                new ChildRowWriter.ParentRecord(config.getTableName(), mainId, primaryData, null),
                joinContext.childRelations(), childDataMap);
        if (childrenChanged) {
            refreshRecordById(config, mainId);
        }
    }

    private void updateJoinedData(LowcodeRuntimeConfig config,
                                  Object id,
                                  Map<String, Object> data,
                                  Set<String> allowedFields,
                                  RuntimeJoinContext joinContext) {
        DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, config.getTableName(), null);
        Map<String, Object> authorizedMainRecord = repository.selectById(config.getTableName(), id, dataScopeCondition);
        if (authorizedMainRecord == null) {
            throw new BusinessException("无权限更新该数据或数据不存在");
        }
        storedFormulaValues.applyUpdate(config, config.getTableName(), id, data, dataScopeCondition, authorizedMainRecord, currentPrimaryKey());
        validateUniqueConstraints(config, config.getTableName(), data, authorizedMainRecord, id);
        ChildRowPolicy.WritePartition partition = ChildRowPolicy.partition(
                data, allowedFields, joinContext.fields(), joinContext.childRelations());
        Map<String, Object> primaryData = partition.primaryData();
        Map<String, Map<String, Object>> childDataMap = partition.childData();
        removePrimaryKeyColumns(primaryData, currentPrimaryKey());
        privacyWrite.removeMaskedColumns(primaryData, config, config.getTableName());

        if (!primaryData.isEmpty()) {
            moneyValues.store(primaryData, config);
            structuredValues.store(primaryData, config);
            privacyWrite.encrypt(primaryData, config.getEncryptConfig());
            int affected = repository.updateById(config.getTableName(), id, primaryData, dataScopeCondition);
            if (affected <= 0) {
                throw new BusinessException("无权限更新该数据或数据不存在");
            }
        }

        boolean childrenChanged = childRowWriter.updateJoined(
                new ChildRowWriter.ParentRecord(config.getTableName(), id, primaryData, authorizedMainRecord),
                joinContext.childRelations(), childDataMap);
        if (primaryData.isEmpty() && childDataMap.values().stream().noneMatch(ChildRowPolicy::hasWritableChildData)) {
            throw new BusinessException("没有可更新的字段");
        }
        if (childrenChanged) {
            refreshRecordById(config, id);
        }
    }

    private Set<String> buildAllowedWriteFields(LowcodeRuntimeConfig config, String tableName) {
        Set<String> fields = formWriteFields.allowed(config, tableName);
        storedFormulaValues.addWriteFields(fields, config);
        fields.removeAll(IMMUTABLE_WRITE_FIELDS);
        return fields;
    }

    /**
     * 构建运行态字段到真实数据库列的映射。
     *
     * <p>低代码字段编码是稳定的业务 API 契约，数据库列名则可能由设计器自动生成，
     * 两者不能假定相同。例如业务字段 {@code dpe} 可以持久化到 {@code field_input4}。
     * 数据库元数据只能提供 camelCase/snake_case 映射，因此这里必须叠加已发布模型中的
     * {@link LowcodeFieldSchema#getColumnName()} 显式映射。</p>
     */
    private Map<String, String> buildRuntimeColumnMapping(LowcodeRuntimeConfig config, String tableName) {
        return writeFieldMapping.columns(config, tableName);
    }



    private void validateUniqueConstraints(LowcodeRuntimeConfig config,
                                           String tableName,
                                           Map<String, Object> data,
                                           Map<String, Object> beforeRecord,
                                           Object excludeId) {
        uniqueValidator.validate(config, tableName, extractMainPayload(data), beforeRecord,
                excludeId, primaryKeyColumn(currentPrimaryKey()));
    }

    // ==================== 删除操作 ====================

    /**
     * 删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String configKey, Object id) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        assertRuntimeWritable(config);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            executeRuntimeWrite(() -> {
                String tableName = config.getTableName();
                LowcodePrimaryKeyStrategy primaryKey = currentPrimaryKey();
                DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, tableName, null);
                Map<String, Object> beforeRecord = repository.selectById(
                        tableName, primaryKeyColumn(primaryKey), id, dataScopeCondition);
                if (beforeRecord == null) {
                    throw new BusinessException("无权限删除该数据或数据不存在");
                }

                // 判断是否逻辑删除
                boolean logicDelete = repository.hasDelFlag(tableName);

                // 执行删除
                int affected = repository.deleteById(tableName, primaryKeyColumn(primaryKey), id, logicDelete, dataScopeCondition);
                if (affected <= 0) {
                    throw new BusinessException("无权限删除该数据或数据不存在");
                }
                storedAggregateRefreshService.refreshRuntimeAfterChildDelete(config, beforeRecord);
                return null;
            });
        }
    }

    /**
     * 暴露运行时配置给动态导入导出服务，仍统一走发布态校验。
     */


    public LowcodeRuntimeConfig getRuntimeValues(String configKey) {
        return getConfig(configKey);
    }

    public Object resolveRecordId(String configKey, Map<String, Object> data) {
        LowcodeRuntimeConfig config = getConfig(configKey);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = useRuntimeContext(config)) {
            return resolvePayloadId(data, currentPrimaryKey());
        }
    }

    private LowcodeRuntimeDataSourceContextHolder.Scope useRuntimeContext(LowcodeRuntimeConfig config) {
        LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolveRuntime(config);
        config.setTableName(StringUtils.defaultIfBlank(context.getTableName(), config.getTableName()));
        return LowcodeRuntimeDataSourceContextHolder.use(context);
    }

    private void assertRuntimeWritable(LowcodeRuntimeConfig config) {
        LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolveRuntime(config);
        if (context.isReadonly() || !context.isAllowWrite()) {
            throw new BusinessException("当前运行数据源为只读或未开放写入");
        }
    }

    private <T> T executeRuntimeWrite(Supplier<T> operation) {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContextHolder.get();
        if (context == null || context.isMaster()) {
            return operation.get();
        }
        if (runtimeWriteTransactions == null) {
            throw new IllegalStateException("External runtime write transaction support is not initialized");
        }
        return runtimeWriteTransactions.execute(context, operation);
    }

    private boolean isMasterDetailRuntime(LowcodeRuntimeConfig config) {
        return config != null && MASTER_DETAIL_LAYOUT.equals(config.getLayoutType());
    }

    private List<DynamicCrudRepository.JoinField> buildRuntimeSelectFields(RuntimeJoinContext joinContext,
                                                                          Collection<String> requestedFields,
                                                                          boolean defaultPrimaryFields) {
        return joinQuery.select(joinContext, requestedFields, defaultPrimaryFields, primaryKeyField(currentPrimaryKey()));
    }

    private LowcodeModelSchema readModelSchema(LowcodeRuntimeConfig config) {
        try {
            return objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析modelSchema失败, configKey={}", config.getConfigKey(), e);
            return null;
        }
    }

    private void applyReadPipeline(List<Map<String, Object>> rows, LowcodeRuntimeConfig config) {
        readPipeline.apply(rows, config);
    }

    // ==================== 配置加载 ====================

    /**
     * 获取配置
     */
    private LowcodeRuntimeConfig getConfig(String configKey) {
        return runtimeConfigs.load(configKey);
    }

    private void validatePublicReferences(LowcodeRuntimeConfig config, Map<String, Object> data, Object id) {
        boolean hasIdentity = new RuntimeIdentityFields(objectMapper).hasPayload(config, data);
        boolean hasTask = RuntimeTaskLinkFields.hasBindings(config, objectMapper);
        if (!hasIdentity && !hasTask) {
            return;
        }
        Map<String, Object> previous = id == null ? null : readRecordByConfig(config, id);
        if (id != null && (previous == null || previous.isEmpty())) {
            throw new BusinessException("无权限更新该数据或数据不存在");
        }
        if (hasIdentity) {
            if (identityReferences == null) {
                throw new BusinessException(UiMessages.label("组织人员引用校验尚未装配", "Identity reference validation is not configured"));
            }
            identityReferences.validateWrite(config, data, previous, id == null ? "add" : "edit");
        }
        if (hasTask) {
            RuntimeTaskLinkFields.validate(config, data, previous, objectMapper);
        }
    }



    private DynamicCrudRepository.SqlCondition buildDataScopeCondition(LowcodeRuntimeConfig config, String tableName, String tableAlias) {
        return dynamicDataScopeService.buildRuntimeCondition(config, tableName, tableAlias);
    }

}
