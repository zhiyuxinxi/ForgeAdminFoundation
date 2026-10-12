package com.mdframe.forge.plugin.generator.vo.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码业务领域树节点。
 */
@Data
public class LowcodeDomainTreeVO {

    private Long id;

    private Long parentId;

    private String domainCode;

    private String domainName;

    private String domainDesc;

    private String icon;

    private Integer sort;

    private String status;

    private List<LowcodeDomainTreeVO> children = new ArrayList<>();
}
