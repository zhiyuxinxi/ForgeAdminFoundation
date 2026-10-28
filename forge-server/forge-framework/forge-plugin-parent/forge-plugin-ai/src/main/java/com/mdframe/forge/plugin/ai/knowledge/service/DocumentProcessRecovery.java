package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.domain.ExpiredDocumentAttempt;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeDocumentMapper;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import com.mdframe.forge.starter.tenant.util.TenantUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Recovers interrupted requests in the primary AI metadata database; never replays external work. */
@Component
@RequiredArgsConstructor
public class DocumentProcessRecovery {
    private final AiKnowledgeDocumentMapper documentMapper;

    @ScheduledJob(name = "knowledgeDocumentRecovery", group = "AI", cron = "0 * * * * ?",
            description = "恢复超时或中断的知识库文档处理 / Recover expired knowledge document attempts")
    public String recoverExpired() {
        List<ExpiredDocumentAttempt> attempts = TenantUtil.ignore(() -> documentMapper.selectExpiredAttempts(100));
        int recovered = 0;
        for (ExpiredDocumentAttempt attempt : attempts) {
            recovered += TenantUtil.with(attempt.tenantId(),
                    () -> documentMapper.expireAttempt(attempt.id(), attempt.attempt()));
        }
        return "recovered=" + recovered;
    }
}
