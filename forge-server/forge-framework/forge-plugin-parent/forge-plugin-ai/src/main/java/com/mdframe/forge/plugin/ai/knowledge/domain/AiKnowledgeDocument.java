package com.mdframe.forge.plugin.ai.knowledge.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 知识库文档
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_knowledge_document")
public class AiKnowledgeDocument extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 知识库ID
     */
    private Long knowledgeId;

    /**
     * 文件ID（FileManager 上传返回的 UUID 字符串）
     */
    private String fileId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 文档类型(pdf/word/excel/markdown/txt/html/url/manual)
     */
    private String docType;

    /**
     * 来源(upload/url/manual/db)
     */
    private String sourceType;

    /**
     * URL来源
     */
    private String sourceUrl;

    /**
     * 内容SHA-256(去重)
     */
    private String contentHash;

    /**
     * 分块数
     */
    private Integer chunkCount;

    /**
     * 处理状态(pending/processing/success/failed)
     */
    private String processStatus;

    /** Internal processing request identity; clients cannot choose the current attempt. */
    @JsonIgnore
    private String processAttempt;

    /**
     * 处理错误信息
     */
    private String processError;

    public String getProcessError() {
        return "knowledge.document.processing.interrupted".equals(processError)
                ? UiMessages.message(processError) : processError;
    }

    /**
     * 删除标志（0正常，删除后写主键）
     */
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
