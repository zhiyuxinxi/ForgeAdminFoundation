package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessQuantityLedger;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessQuantityLock;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessQuantityOperationDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessQuantityLockMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/** Matches persisted quantity effects before replaying a receipt, without executing another write. */
final class BusinessQuantityReplay {
    private BusinessQuantityReplay() {
    }

    static void requireMatch(Long tenantId, BusinessQuantityOperationDTO request,
                             AiBusinessQuantityLedger ledger, BusinessQuantityLockMapper locks) {
        AiBusinessQuantityLock lock = replayLock(tenantId, request, ledger, locks);
        boolean consumesLock = "COMMIT".equals(request.getOperationType()) || "RELEASE".equals(request.getOperationType());
        List<String> requestedSource = source(request.getSourceObjectCode(), request.getSourceRecordId(),
                request.getSourceDetailId(), consumesLock ? lock : null);
        long signedQuantity = "INBOUND".equals(request.getOperationType()) || "LOCK".equals(request.getOperationType())
                ? request.getQuantity() : -request.getQuantity();
        Effect expected = new Effect(request.getOperationType(), key(request.getAccountCode(), request.getItemCode(), request.getDimensionKey()),
                signedQuantity, key(request.getTargetAccountCode(), request.getTargetItemCode(), request.getTargetDimensionKey()), requestedSource);
        Effect recorded = new Effect(ledger.getOperationType(), key(ledger.getAccountCode(), ledger.getItemCode(), ledger.getDimensionKey()),
                ledger.getQuantityDelta(), key(ledger.getTargetAccountCode(), ledger.getTargetItemCode(), ledger.getTargetDimensionKey()),
                source(ledger.getSourceObjectCode(), ledger.getSourceRecordId(), ledger.getSourceDetailId(), null));
        if (!expected.equals(recorded)) {
            throw mismatch();
        }
    }

    private static AiBusinessQuantityLock replayLock(Long tenantId, BusinessQuantityOperationDTO request,
                                                     AiBusinessQuantityLedger ledger, BusinessQuantityLockMapper locks) {
        if (ledger.getLockId() == null) {
            if (request.getLockId() != null || StringUtils.isNotBlank(request.getLockCode())) {
                throw mismatch();
            }
            return null;
        }
        AiBusinessQuantityLock lock = locks.selectById(ledger.getLockId());
        if (lock == null || !Objects.equals(tenantId, lock.getTenantId())
                || (request.getLockId() != null && !Objects.equals(request.getLockId(), lock.getId()))
                || (StringUtils.isNotBlank(request.getLockCode())
                    && !StringUtils.equals(StringUtils.trim(request.getLockCode()), lock.getLockCode()))) {
            throw mismatch();
        }
        return lock;
    }

    private static List<String> source(String object, String record, String detail, AiBusinessQuantityLock fallback) {
        return key(StringUtils.defaultIfBlank(object, fallback == null ? null : fallback.getSourceObjectCode()),
                StringUtils.defaultIfBlank(record, fallback == null ? null : fallback.getSourceRecordId()),
                StringUtils.defaultIfBlank(detail, fallback == null ? null : fallback.getSourceDetailId()));
    }

    private static List<String> key(String first, String second, String third) {
        return List.of(StringUtils.trimToEmpty(first), StringUtils.trimToEmpty(second), StringUtils.trimToEmpty(third));
    }

    private static BusinessException mismatch() {
        return new BusinessException(UiMessages.label("数量台账幂等键已用于不同操作参数", "Quantity ledger idempotency key was already used for different operation parameters"));
    }

    private record Effect(String operation, List<String> account, Long delta, List<String> target, List<String> source) {
    }
}
