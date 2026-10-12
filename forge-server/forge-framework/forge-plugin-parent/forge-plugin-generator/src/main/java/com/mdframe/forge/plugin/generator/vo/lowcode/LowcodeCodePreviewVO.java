package com.mdframe.forge.plugin.generator.vo.lowcode;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 低代码应用代码预览结果。
 */
@Data
public class LowcodeCodePreviewVO {

    private Long appId;

    private String configKey;

    private String sourceType;

    private Long versionId;

    private Integer fileCount;

    private Map<String, String> files = new LinkedHashMap<>();
}

