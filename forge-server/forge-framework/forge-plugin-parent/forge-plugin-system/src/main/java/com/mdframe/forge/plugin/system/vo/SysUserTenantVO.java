package com.mdframe.forge.plugin.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户可访问租户视图
 */
@Data
public class SysUserTenantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long tenantId;

    private String tenantName;

    private Integer tenantStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    private Integer memberType;

    private Integer isDefault;

    private Integer status;
}
