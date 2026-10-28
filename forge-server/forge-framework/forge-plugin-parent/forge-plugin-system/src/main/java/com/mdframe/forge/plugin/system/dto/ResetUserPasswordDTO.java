package com.mdframe.forge.plugin.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetUserPasswordDTO {

    @NotNull(message = "用户不能为空")
    private Long id;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "密码不能为空")
    private String password;
}
