package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessQuantityLedger;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessQuantityLock;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessQuantityOperationDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessQuantityBalanceMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessQuantityLedgerMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessQuantityLockMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessQuantitySafetyTest {
    private final BusinessQuantityBalanceMapper balances = mock(BusinessQuantityBalanceMapper.class);
    private final BusinessQuantityLedgerMapper ledgers = mock(BusinessQuantityLedgerMapper.class);
    private final BusinessQuantityLockMapper locks = mock(BusinessQuantityLockMapper.class);
    private final BusinessQuantityLedgerService writes = new BusinessQuantityLedgerService(new ObjectMapper(), balances, ledgers, locks);
    private final BusinessQuantityQueryService queries = new BusinessQuantityQueryService(balances, ledgers, locks);
    private MockedStatic<SessionHelper> session;

    @BeforeEach
    void openSession() {
        session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getTenantId).thenReturn(37L);
    }

    @AfterEach
    void closeSession() {
        session.close();
    }

    @Test
    void missingOrInvalidTenantCannotReadOrWriteDefaultTenant() {
        for (Long tenant : Arrays.asList(null, 0L, -1L)) {
            session.when(SessionHelper::getTenantId).thenReturn(tenant);
            assertThrows(BusinessException.class, () -> writes.inbound(request()));
            assertThrows(BusinessException.class, () -> queries.selectBalancePage(null));
            assertThrows(BusinessException.class, () -> queries.selectLedgerPage(null));
            assertThrows(BusinessException.class, () -> queries.selectLockPage(null));
        }
        verifyNoInteractions(balances, ledgers, locks);
    }

    @Test
    void tenantResolutionFailureRetainsCauseWithoutFallingBack() {
        IllegalStateException failure = new IllegalStateException("test session unavailable");
        session.when(SessionHelper::getTenantId).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> writes.inbound(request())).getCause());
        assertSame(failure, assertThrows(BusinessException.class, () -> queries.selectBalancePage(null)).getCause());
        verifyNoInteractions(balances, ledgers, locks);
    }

    @Test
    void authenticatedNonDefaultTenantIsPassedToTheMapper() {
        queries.selectBalancePage(null);
        verify(balances).selectBalancePage(any(), eq(37L), any());
        verifyNoInteractions(ledgers, locks);
    }

    @Test
    void sameKeyWithChangedQuantityIdentityIsRejectedBeforeReadingOrChangingBalances() {
        when(ledgers.selectByIdempotencyKey(37L, "K1")).thenReturn(ledger("INBOUND", 10L));
        List<Consumer<BusinessQuantityOperationDTO>> changes = List.of(
                dto -> dto.setAccountCode("other"), dto -> dto.setItemCode("other"),
                dto -> dto.setDimensionKey("other"), dto -> dto.setQuantity(11L),
                dto -> dto.setSourceObjectCode("other"), dto -> dto.setSourceRecordId("other"),
                dto -> dto.setSourceDetailId("other"), dto -> dto.setTargetAccountCode("other"));
        for (Consumer<BusinessQuantityOperationDTO> change : changes) {
            BusinessQuantityOperationDTO dto = request();
            change.accept(dto);
            assertThrows(BusinessException.class, () -> writes.inbound(dto));
        }
        assertThrows(BusinessException.class, () -> writes.lock(request()));
        verifyNoInteractions(balances, locks);
        verify(ledgers, never()).insert(any(AiBusinessQuantityLedger.class));
    }

    @Test
    void transferTargetReceiptCannotBeReplayedAsTheSourceOperation() {
        AiBusinessQuantityLedger targetReceipt = ledger("TRANSFER", 10L);
        targetReceipt.setTargetAccountCode("target");
        targetReceipt.setTargetItemCode("sku");
        targetReceipt.setTargetDimensionKey("");
        when(ledgers.selectByIdempotencyKey(37L, "K1")).thenReturn(targetReceipt);
        BusinessQuantityOperationDTO dto = request();
        dto.setTargetAccountCode("target");
        assertThrows(BusinessException.class, () -> writes.transfer(dto));
        verifyNoInteractions(balances, locks);
    }

    @Test
    void completedLockAllowsEquivalentCommitReplayButRejectsDifferentLockOrSource() {
        AiBusinessQuantityLedger receipt = ledger("COMMIT", -10L);
        receipt.setLockId(50L);
        receipt.setSourceObjectCode("order");
        receipt.setSourceRecordId("record-7");
        AiBusinessQuantityLock originalLock = new AiBusinessQuantityLock();
        originalLock.setId(50L);
        originalLock.setTenantId(37L);
        originalLock.setLockStatus("COMMITTED");
        originalLock.setLockCode("original-lock");
        originalLock.setSourceObjectCode("order");
        originalLock.setSourceRecordId("record-7");
        when(ledgers.selectByIdempotencyKey(37L, "K1")).thenReturn(receipt);
        when(locks.selectById(50L)).thenReturn(originalLock);
        BusinessQuantityOperationDTO dto = request();
        dto.setLockId(50L);
        assertTrue(writes.commit(dto).getIdempotentHit());
        dto.setLockId(51L);
        assertThrows(BusinessException.class, () -> writes.commit(dto));
        dto.setLockId(50L);
        dto.setSourceRecordId("different-source");
        assertThrows(BusinessException.class, () -> writes.commit(dto));
        dto.setSourceRecordId(null);
        dto.setLockCode("different-lock");
        assertThrows(BusinessException.class, () -> writes.commit(dto));
        verify(balances, never()).commitLockedQuantity(any(), any(), any(), any(), any());
        verify(locks, never()).commitLockQuantity(any(), any(), any());
        verify(ledgers, never()).insert(any(AiBusinessQuantityLedger.class));
    }

    private BusinessQuantityOperationDTO request() {
        BusinessQuantityOperationDTO dto = new BusinessQuantityOperationDTO();
        dto.setAccountCode("main");
        dto.setItemCode("sku");
        dto.setQuantity(10L);
        dto.setIdempotencyKey("K1");
        return dto;
    }

    private AiBusinessQuantityLedger ledger(String operation, Long delta) {
        AiBusinessQuantityLedger ledger = new AiBusinessQuantityLedger();
        ledger.setId(10L);
        ledger.setTenantId(37L);
        ledger.setOperationType(operation);
        ledger.setAccountCode("main");
        ledger.setItemCode("sku");
        ledger.setDimensionKey("");
        ledger.setQuantityDelta(delta);
        ledger.setBalanceQuantity(10L);
        ledger.setLockedQuantity(0L);
        return ledger;
    }
}
