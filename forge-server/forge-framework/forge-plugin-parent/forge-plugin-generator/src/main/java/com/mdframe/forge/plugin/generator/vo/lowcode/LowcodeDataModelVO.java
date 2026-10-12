package com.mdframe.forge.plugin.generator.vo.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 低代码数据模型详情。
 */
@Data
public class LowcodeDataModelVO {

    private Long id;

    private Long domainId;

    private String domainCode;

    private String domainName;

    private String modelCode;

    private String modelName;

    private String modelDesc;

    private String status;

    private Boolean tenantEnabled;

    private Boolean masterData;

    private LowcodeModelSchema modelSchema;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
