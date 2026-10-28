package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowCc;
import com.mdframe.forge.starter.flow.service.FlowCcService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FlowCcControllerSecurityTest {
    private final FlowCcService cc = mock(FlowCcService.class);
    private final FlowTaskService tasks = mock(FlowTaskService.class);
    private final FlowCcController controller = new FlowCcController(cc, tasks);

    @Test
    void sendingIgnoresForgedSenderIdentity() {
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getUserId).thenReturn(7L);
            session.when(SessionHelper::getUsername).thenReturn("current-user");
            controller.sendCc(Map.of("sendUserId", "999", "sendUserName", "forged-user",
                    "ccUserIds", List.of("8"), "ccUserNames", List.of("recipient")));
            verify(cc).sendCc(null, null, null, null, null, null,
                    List.of("8"), List.of("recipient"), "7", "current-user");
        }
    }

    @Test
    void listsAndCountsUseTheCurrentSessionIdentity() {
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getUserId).thenReturn(7L);
            controller.myCc(1, 10, 0);
            controller.sentCc(1, 10);
            controller.countUnread();
            verify(cc).myCc(any(), eq("7"), eq(0));
            verify(cc).sentCc(any(), eq("7"));
            verify(cc).countUnread("7");
        }
    }

    @Test
    void anotherRecipientsRecordCannotBeReadOrMarkedRead() {
        when(cc.getById("cc-1")).thenReturn(record("cc-1", "8"));
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getUserId).thenReturn(7L);
            assertThat(controller.getCcFormInfo("cc-1").getCode()).isNotEqualTo(200);
            assertThat(controller.markRead("cc-1").getCode()).isNotEqualTo(200);
            verify(cc, never()).markRead(any());
            verifyNoInteractions(tasks);
        }
    }

    @Test
    void batchReadRejectsMixedOwnershipWithoutPartialMutation() {
        when(cc.listByIds(any())).thenReturn(List.of(record("mine", "7"), record("other", "8")));
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getUserId).thenReturn(7L);
            assertThat(controller.batchMarkRead(List.of("mine", "other")).getCode()).isNotEqualTo(200);
            verify(cc, never()).batchMarkRead(any());
        }
    }

    @Test
    void batchReadRequiresEveryRequestedRecordAndAcceptsOwnedDuplicatesOnce() {
        when(cc.listByIds(any())).thenReturn(List.of(record("mine", "7")));
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getUserId).thenReturn(7L);
            assertThat(controller.batchMarkRead(List.of("mine", "missing")).getCode()).isNotEqualTo(200);
            verify(cc, never()).batchMarkRead(any());
            assertThat(controller.batchMarkRead(List.of("mine", "mine")).getCode()).isEqualTo(200);
            verify(cc).batchMarkRead(List.of("mine"));
        }
    }

    private FlowCc record(String id, String recipient) {
        FlowCc result = new FlowCc();
        result.setId(id);
        result.setCcUserId(recipient);
        result.setTenantId(1L);
        return result;
    }
}
