package com.mdframe.forge.plugin.system.vo;


import com.mdframe.forge.starter.trans.annotation.DictTrans;
import com.mdframe.forge.starter.trans.annotation.TransField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@DictTrans
public class SysOrgTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private String tenantName;

    private String orgName;

    private Long parentId;

    private String ancestors;

    private Integer sort;

    @TransField(dictType = "sys_org_type")
    private String orgType;

    private String orgTypeName;

    @TransField(dictType = "sys_normal_disable")
    private Integer orgStatus;

    private String orgStatusName;

    private Long leaderId;

    private String leaderName;

    private String phone;

    private String address;

    private String regionCode;

    private String orgCode;

    private String orgShortName;

    private String orgParentCode;

    private String orgPath;

    private String levelId;

    private String remark;

    private Boolean hasChildren;

    private List<SysOrgTreeVO> children;
}
