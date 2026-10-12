package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户岗位绑定DTO
 */
@Data
public class UserPostBindDTO {

    /**
     * 岗位ID列表
     */
    private List<Long> postIds;

    /**
     * 主岗位ID
     */
    private Long mainPostId;
}
