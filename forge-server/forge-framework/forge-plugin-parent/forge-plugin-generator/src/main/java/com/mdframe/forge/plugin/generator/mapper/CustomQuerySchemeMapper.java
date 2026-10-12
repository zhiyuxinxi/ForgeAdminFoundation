package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.generator.domain.entity.CustomQueryScheme;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomQuerySchemeMapper extends BaseMapper<CustomQueryScheme> {

    List<CustomQueryScheme> selectUserSchemes(@Param("tenantId") Long tenantId,
                                              @Param("userId") Long userId,
                                              @Param("configKey") String configKey);

    CustomQueryScheme selectUserScheme(@Param("tenantId") Long tenantId,
                                       @Param("userId") Long userId,
                                       @Param("configKey") String configKey,
                                       @Param("id") Long id);

    int clearDefault(@Param("tenantId") Long tenantId,
                     @Param("userId") Long userId,
                     @Param("configKey") String configKey);

    int deleteUserScheme(@Param("tenantId") Long tenantId,
                         @Param("userId") Long userId,
                         @Param("configKey") String configKey,
                         @Param("id") Long id);
}
