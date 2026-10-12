package com.mdframe.forge.plugin.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户组织绑定详情。
 */
@Data
public class UserOrgBindingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private Long userId;

    private Long orgId;

    private String orgName;

    private Long parentId;

    private String ancestors;

    private Integer isMain;

    private Integer roleCount;

    private List<String> roleNames;
}
