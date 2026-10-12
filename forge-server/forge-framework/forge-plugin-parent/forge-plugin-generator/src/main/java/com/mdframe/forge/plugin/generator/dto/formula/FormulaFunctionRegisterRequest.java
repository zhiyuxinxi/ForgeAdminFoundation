package com.mdframe.forge.plugin.generator.dto.formula;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 公式自定义函数注册请求。
 */
@Data
public class FormulaFunctionRegisterRequest {

    @NotBlank(message = "函数编码不能为空")
    @Pattern(regexp = "^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*$",
            message = "函数编码只能包含字母、数字、下划线和点号，且不能以数字开头")
    private String functionCode;

    @NotBlank(message = "展示名称不能为空")
    private String displayName;

    @NotBlank(message = "函数分类不能为空")
    private String category;

    private String description;

    private String argumentSchema;

    private String returnType;

    private String example;

    private String version;

    @NotBlank(message = "Bean 名称不能为空")
    private String beanName;

    @NotBlank(message = "方法名称不能为空")
    private String methodName;

    private String releaseNote;

    private Boolean enabled;
}
