package com.mdframe.forge.plugin.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.data.entity.DataBusinessDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataBusinessDefinitionMapper extends BaseMapper<DataBusinessDefinition> {

    IPage<DataBusinessDefinition> selectBusinessPage(Page<DataBusinessDefinition> page,
        @Param("tenantId") Long tenantId,
        @Param("businessName") String businessName,
        @Param("status") Integer status);

    List<DataBusinessDefinition> selectBusinessList(@Param("tenantId") Long tenantId,
        @Param("status") Integer status);

    DataBusinessDefinition selectBusinessByCode(@Param("tenantId") Long tenantId,
        @Param("businessCode") String businessCode);
}
