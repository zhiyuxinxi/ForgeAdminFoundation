package com.mdframe.forge.starter.auth.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "当前密码不能为空")
    private String oldPassword;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "新密码不能为空")
    private String newPassword;
}
