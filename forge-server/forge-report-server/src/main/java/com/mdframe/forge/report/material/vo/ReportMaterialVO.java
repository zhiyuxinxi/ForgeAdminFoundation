package com.mdframe.forge.report.material.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表素材视图对象。
 */
@Data
public class ReportMaterialVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String fileId;

    private String originalName;

    private Long fileSize;

    private String mimeType;

    private String extension;

    private String storageType;

    private String businessType;

    private String businessId;

    private Boolean isPrivate;

    private String accessUrl;

    private LocalDateTime uploadTime;

    private Integer downloadCount;
}
