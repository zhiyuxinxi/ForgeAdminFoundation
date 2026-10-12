package com.mdframe.forge.starter.datascope.mapper;

import com.mdframe.forge.starter.datascope.model.DataScopeRegionInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 行政区划元数据 Mapper。
 */
@Mapper
public interface DataScopeRegionMapper {

    /**
     * 查询全部行政区划父子关系，用于构建平台元数据快照。
     */
    List<DataScopeRegionInfo> selectAllRegions();
}
