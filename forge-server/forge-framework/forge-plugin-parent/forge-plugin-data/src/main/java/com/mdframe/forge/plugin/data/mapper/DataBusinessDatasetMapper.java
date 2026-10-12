package com.mdframe.forge.plugin.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.data.entity.DataBusinessDataset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataBusinessDatasetMapper extends BaseMapper<DataBusinessDataset> {

    List<DataBusinessDataset> selectByBusinessId(@Param("tenantId") Long tenantId,
        @Param("businessId") Long businessId);

    int deleteByBusinessId(@Param("tenantId") Long tenantId,
        @Param("businessId") Long businessId);
}
