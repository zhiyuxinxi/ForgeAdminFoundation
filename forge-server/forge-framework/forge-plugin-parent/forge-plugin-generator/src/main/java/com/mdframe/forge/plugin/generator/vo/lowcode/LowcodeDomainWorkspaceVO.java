package com.mdframe.forge.plugin.generator.vo.lowcode;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 低代码业务领域工作台。
 */
@Data
public class LowcodeDomainWorkspaceVO {

    private LowcodeDomainVO domain;

    private Long appCount = 0L;

    private Long publishedCount = 0L;

    private Long draftCount = 0L;

    private Long objectCount = 0L;

    private List<ObjectOverviewVO> objects = new ArrayList<>();

    private List<RecentVersionVO> recentVersions = new ArrayList<>();

    @Data
    public static class ObjectOverviewVO {

        private String objectCode;

        private String objectName;

        private String appName;

        private String configKey;

        private String publishStatus;

        private LocalDateTime updateTime;
    }

    @Data
    public static class RecentVersionVO {

        private Long id;

        private Long configId;

        private String configKey;

        private String appName;

        private String objectCode;

        private String objectName;

        private Integer versionNo;

        private String versionType;

        private String remark;

        private LocalDateTime createTime;

        private Long createBy;
    }
}
