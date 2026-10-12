package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.lowcode.formula.runtime.port.FormulaExecutionTraceSink;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaExecutionLog;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaExecutionLogQueryDTO;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaExecutionLogResponse;
import com.mdframe.forge.plugin.generator.mapper.FormulaExecutionLogMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公式执行日志服务。
 */
@Service
public class FormulaExecutionLogService implements FormulaExecutionTraceSink {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private final FormulaExecutionLogMapper formulaExecutionLogMapper;
    private final FormulaRuntimeProperties runtimeProperties;
    private final FormulaValueMasker valueMasker;

    public FormulaExecutionLogService(FormulaExecutionLogMapper formulaExecutionLogMapper) {
        this(formulaExecutionLogMapper, new FormulaRuntimeProperties(), new FormulaValueMasker());
    }

    @Autowired
    public FormulaExecutionLogService(FormulaExecutionLogMapper formulaExecutionLogMapper,
                                      FormulaRuntimeProperties runtimeProperties,
                                      FormulaValueMasker valueMasker) {
        this.formulaExecutionLogMapper = formulaExecutionLogMapper;
        this.runtimeProperties = runtimeProperties == null ? new FormulaRuntimeProperties() : runtimeProperties;
        this.valueMasker = valueMasker == null ? new FormulaValueMasker() : valueMasker;
    }

    public void record(FormulaExecutionLogResponse log) {
        if (log == null || !runtimeProperties.isExecutionLogEnabled()) {
            return;
        }
        formulaExecutionLogMapper.insert(toEntity(log));
    }

    public Page<FormulaExecutionLogResponse> page(FormulaExecutionLogQueryDTO query) {
        FormulaExecutionLogQueryDTO effectiveQuery = query == null ? new FormulaExecutionLogQueryDTO() : query;
        Page<AiFormulaExecutionLog> page = new Page<>(effectiveQuery.getPageNum(), effectiveQuery.getPageSize());
        Page<AiFormulaExecutionLog> result = formulaExecutionLogMapper.selectFormulaExecutionLogPage(
                page, resolveTenantId(), effectiveQuery);
        Page<FormulaExecutionLogResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<FormulaExecutionLogResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .toList();
        responsePage.setRecords(records);
        return responsePage;
    }

    public FormulaExecutionLogResponse detail(Long id) {
        if (id == null) {
            return null;
        }
        AiFormulaExecutionLog entity = formulaExecutionLogMapper.selectFormulaExecutionLogById(resolveTenantId(), id);
        return toResponse(entity);
    }

    private AiFormulaExecutionLog toEntity(FormulaExecutionLogResponse source) {
        AiFormulaExecutionLog entity = new AiFormulaExecutionLog();
        entity.setId(source.getId());
        entity.setTenantId(source.getTenantId() == null ? resolveTenantId() : source.getTenantId());
        entity.setTraceId(source.getTraceId());
        entity.setObjectCode(source.getObjectCode());
        entity.setRecordId(source.getRecordId());
        entity.setFieldCode(source.getFieldCode());
        entity.setFormulaType(source.getFormulaType());
        entity.setFormulaMode(source.getFormulaMode());
        entity.setExpression(source.getExpression());
        entity.setInputSnapshot(valueMasker.mask(source.getInputSnapshot()));
        entity.setOutputValue(valueMasker.mask(source.getOutputValue()));
        entity.setSuccess(Boolean.TRUE.equals(source.getSuccess()));
        entity.setErrorMessage(source.getErrorMessage());
        entity.setElapsedMs(source.getElapsedMs());
        fillAuditFields(entity, source);
        return entity;
    }

    private FormulaExecutionLogResponse toResponse(AiFormulaExecutionLog source) {
        if (source == null) {
            return null;
        }
        return FormulaExecutionLogResponse.builder()
                .id(source.getId())
                .tenantId(source.getTenantId())
                .traceId(source.getTraceId())
                .objectCode(source.getObjectCode())
                .recordId(source.getRecordId())
                .fieldCode(source.getFieldCode())
                .formulaType(source.getFormulaType())
                .formulaMode(source.getFormulaMode())
                .expression(source.getExpression())
                .inputSnapshot(source.getInputSnapshot())
                .outputValue(source.getOutputValue())
                .success(source.getSuccess())
                .errorMessage(source.getErrorMessage())
                .elapsedMs(source.getElapsedMs())
                .createBy(source.getCreateBy())
                .createTime(source.getCreateTime())
                .createDept(source.getCreateDept())
                .updateBy(source.getUpdateBy())
                .updateTime(source.getUpdateTime())
                .build();
    }

    private void fillAuditFields(AiFormulaExecutionLog target, FormulaExecutionLogResponse source) {
        Long userId = resolveUserId();
        Long deptId = resolveMainOrgId();
        LocalDateTime now = LocalDateTime.now();
        target.setCreateBy(source.getCreateBy() == null ? userId : source.getCreateBy());
        target.setCreateTime(source.getCreateTime() == null ? now : source.getCreateTime());
        target.setCreateDept(source.getCreateDept() == null ? deptId : source.getCreateDept());
        target.setUpdateBy(source.getUpdateBy() == null ? userId : source.getUpdateBy());
        target.setUpdateTime(source.getUpdateTime() == null ? now : source.getUpdateTime());
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
        } catch (Exception e) {
            return DEFAULT_TENANT_ID;
        }
    }

    private Long resolveUserId() {
        try {
            return SessionHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveMainOrgId() {
        try {
            return SessionHelper.getMainOrgId();
        } catch (Exception e) {
            return null;
        }
    }

}
