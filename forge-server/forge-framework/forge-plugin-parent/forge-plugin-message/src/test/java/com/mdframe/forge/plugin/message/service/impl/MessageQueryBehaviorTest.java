package com.mdframe.forge.plugin.message.service.impl;

import com.mdframe.forge.plugin.message.domain.dto.MessageManageQueryDTO;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageTemplate;
import com.mdframe.forge.plugin.message.security.MessageAccess;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class MessageQueryBehaviorTest {
    @Test
    void messageFiltersKeepGroupingTenantStatusInclusiveTimesAndPagination() throws Exception {
        try (var f = new MessageQueryFixture(); var identity = manager()) {
            seedMessages(f);
            MessageManageQueryDTO query = new MessageManageQueryDTO();
            query.setType("SYSTEM");
            query.setChannel("WEB");
            query.setStatus(0);
            query.setKeyword("needle");
            query.setStartTime("2026-10-09 10:00:00");
            query.setEndTime("2026-10-09 12:00:00");
            var page = f.management.pageMessages(query, 2, 1);
            assertEquals(2, page.getTotal());
            assertEquals(List.of(1L), page.getRecords().stream().map(v -> v.getId()).toList());
            assertEquals(0, page.getRecords().get(0).getReceiverCount());
        }
    }

    @Test
    void messageBlankParametersAndInvalidTimeKeepOriginalBehavior() throws Exception {
        try (var f = new MessageQueryFixture(); var identity = manager()) {
            seedMessages(f);
            MessageManageQueryDTO query = new MessageManageQueryDTO();
            query.setType(" \t");
            query.setChannel("");
            query.setKeyword("　");
            query.setStartTime(" ");
            query.setEndTime("");
            assertEquals(4, f.management.pageMessages(query, 1, 10).getTotal());
            query.setStartTime("not-a-date");
            assertThrows(DateTimeParseException.class, () -> f.management.pageMessages(query, 1, 10));
            assertThrows(NullPointerException.class, () -> f.management.pageMessages(null, 1, 10));
        }
    }

    @Test
    void detailKeepsTenantIsolationAndDuplicateRecordFailure() throws Exception {
        try (var f = new MessageQueryFixture(); var identity = manager()) {
            seedMessages(f);
            f.jdbc.update("INSERT INTO sys_message_send_record(id,tenant_id,message_id) VALUES(1,20,1),(2,99,1)");
            assertEquals(1L, f.management.getMessageDetail(1L).getSendRecord().getId());
            assertNull(f.management.getMessageDetail(3L));
            f.jdbc.update("INSERT INTO sys_message_send_record(id,tenant_id,message_id) VALUES(3,20,1)");
            f.session.clearCache();
            assertThrows(org.apache.ibatis.exceptions.TooManyResultsException.class,
                    () -> f.management.getMessageDetail(1L));
        }
    }

    @Test
    void templateQueriesKeepDeletionTenantWhitespaceWildcardsAndNullCode() throws Exception {
        try (var f = new MessageQueryFixture()) {
            seedTemplates(f);
            assertNull(f.templates.getByCode(null));
            assertNull(f.templates.getByCode("DELETED"));
            assertEquals(1L, f.templates.getByCode("A_1").getId());
            var page = f.templates.page("SYSTEM", "needle", 2, 1);
            assertEquals(2, page.getTotal());
            assertEquals(1L, page.getRecords().get(0).getId());
            assertEquals(3, f.templates.page("　", " \t", 1, 10).getTotal());
            assertEquals(3, f.templates.page(null, "%", 1, 10).getTotal());
            assertEquals(1, f.templates.page(null, "A_1", 1, 10).getTotal());
        }
    }

    @Test
    void templateCreateAndUpdateKeepCodeUniquenessAndFailureSemantics() throws Exception {
        try (var f = new MessageQueryFixture()) {
            seedTemplates(f);
            SysMessageTemplate template = new SysMessageTemplate();
            org.springframework.beans.BeanUtils.copyProperties(f.templates.getById(1L), template);
            assertThrows(RuntimeException.class, () -> f.templates.create(template));
            template.setTemplateCode("B");
            assertThrows(RuntimeException.class, () -> f.templates.update(template));
            template.setTemplateCode("RENAMED");
            f.templates.update(template);
            assertEquals(1L, f.templates.getByCode("RENAMED").getId());
            template.setTemplateCode("DELETED");
            f.templates.update(template);
            assertEquals(1L, f.templates.getByCode("DELETED").getId());
        }
    }

    @Test
    void existingMapperDataScopeStillFiltersRowsAndPageTotals() throws Exception {
        try (var f = new MessageQueryFixture(); var identity = manager()) {
            seedMessages(f);
            seedTemplates(f);
            f.restrictToCreator();
            assertEquals(3, f.management.pageMessages(new MessageManageQueryDTO(), 1, 10).getTotal());
            assertEquals(2, f.templates.page(null, null, 1, 10).getTotal());
        }
    }

    private MockedStatic<MessageAccess> manager() {
        MockedStatic<MessageAccess> access = mockStatic(MessageAccess.class);
        access.when(() -> MessageAccess.requirePermission(MessageAccess.QUERY)).thenReturn(20L);
        return access;
    }

    private void seedMessages(MessageQueryFixture f) {
        f.jdbc.update("INSERT INTO sys_message(id,tenant_id,title,content,type,send_channel,status,create_time,create_by) VALUES"
                + "(1,20,'needle','x','SYSTEM','WEB',0,'2026-10-09 10:00:00',10),"
                + "(2,20,'x','needle','SYSTEM','WEB',0,'2026-10-09 12:00:00',11),"
                + "(3,99,'needle','needle','SYSTEM','WEB',0,'2026-10-09 11:00:00',10),"
                + "(4,20,'needle','needle','SMS','SMS',1,'2026-10-09 11:00:00',10),"
                + "(5,20,'x','x','SYSTEM','WEB',0,'2026-10-09 13:00:00',10)");
    }

    private void seedTemplates(MessageQueryFixture f) {
        f.jdbc.update("INSERT INTO sys_message_template(id,tenant_id,template_code,template_name,type,del_flag,create_time,create_by) VALUES"
                + "(1,20,'A_1','needle','SYSTEM',0,'2026-10-09 10:00:00',10),"
                + "(2,20,'B','needle','SYSTEM',0,'2026-10-09 12:00:00',11),"
                + "(3,99,'A_1','needle','SYSTEM',0,'2026-10-09 11:00:00',10),"
                + "(4,20,'DELETED','needle','SYSTEM',4,'2026-10-09 11:00:00',10),"
                + "(5,20,'C','x','SMS',0,'2026-10-09 13:00:00',10)");
    }
}
