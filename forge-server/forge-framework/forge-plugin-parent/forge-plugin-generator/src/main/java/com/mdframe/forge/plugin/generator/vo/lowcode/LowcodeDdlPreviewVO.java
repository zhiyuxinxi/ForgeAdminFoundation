package com.mdframe.forge.plugin.generator.vo.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码 DDL 预览结果。
 */
@Data
public class LowcodeDdlPreviewVO {

    private String tableName;

    private Boolean tableExists;

    private Boolean executable;

    private List<String> ddlStatements = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();
}
