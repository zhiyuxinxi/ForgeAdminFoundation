package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-组织关联Mapper接口
 */
@Mapper
public interface SysUserOrgMapper extends BaseMapper<SysUserOrg> {

    /**
     * 查询用户在指定租户下显式绑定的组织ID。
     */
    List<Long> selectOrgIdsByUserTenant(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

}
