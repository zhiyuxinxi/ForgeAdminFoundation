package com.mdframe.forge.plugin.ai.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledgeDocument;
import com.mdframe.forge.plugin.ai.knowledge.domain.ExpiredDocumentAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 知识库文档 Mapper
 */
@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocument> {

    Page<AiKnowledgeDocument> selectDocumentPage(Page<AiKnowledgeDocument> page,
                                                  @Param("knowledgeId") Long knowledgeId,
                                                  @Param("docName") String docName,
                                                  @Param("processStatus") String processStatus);

    List<AiKnowledgeDocument> selectByKnowledgeId(@Param("knowledgeId") Long knowledgeId);

    AiKnowledgeDocument selectByIdForUpdate(@Param("id") Long id);

    int requestProcessing(@Param("id") Long id, @Param("expectedStatus") String expectedStatus,
                          @Param("attempt") String attempt, @Param("timeoutSeconds") int timeoutSeconds);

    int claimPendingDocument(@Param("id") Long id, @Param("attempt") String attempt,
                             @Param("timeoutSeconds") int timeoutSeconds);

    List<ExpiredDocumentAttempt> selectExpiredAttempts(@Param("limit") int limit);

    int expireAttempt(@Param("id") Long id, @Param("attempt") String attempt);

    long countByContentHash(@Param("knowledgeId") Long knowledgeId,
                            @Param("contentHash") String contentHash,
                            @Param("excludeId") Long excludeId);

    int finishAttempt(@Param("id") Long id, @Param("attempt") String attempt,
                      @Param("expectedStatus") String expectedStatus,
                      @Param("processStatus") String processStatus,
                      @Param("processError") String processError,
                      @Param("chunkCount") Integer chunkCount);
}
