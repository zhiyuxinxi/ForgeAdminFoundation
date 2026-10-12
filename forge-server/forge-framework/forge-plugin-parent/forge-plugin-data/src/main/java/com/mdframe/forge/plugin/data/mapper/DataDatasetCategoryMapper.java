package com.mdframe.forge.plugin.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.data.entity.DataDatasetCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataDatasetCategoryMapper extends BaseMapper<DataDatasetCategory> {

    List<DataDatasetCategory> selectCategoryList(@Param("tenantId") Long tenantId,
        @Param("categoryName") String categoryName,
        @Param("status") Integer status);

    DataDatasetCategory selectCategoryByCode(@Param("tenantId") Long tenantId,
        @Param("categoryCode") String categoryCode);

    int selectChildCount(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);
}
