package com.mdframe.forge.plugin.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户组织角色摘要。
 */
@Data
public class UserOrgRoleSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer roleCount;

    private List<String> roleNames;
}
