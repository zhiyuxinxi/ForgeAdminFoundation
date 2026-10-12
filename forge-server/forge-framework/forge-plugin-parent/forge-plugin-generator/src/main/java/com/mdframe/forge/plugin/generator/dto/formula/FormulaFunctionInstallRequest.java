package com.mdframe.forge.plugin.generator.dto.formula;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公式函数安装请求。
 */
@Data
public class FormulaFunctionInstallRequest {

    @NotBlank(message = "函数编码不能为空")
    private String functionCode;

    private String version;

    private Boolean enabled;
}
