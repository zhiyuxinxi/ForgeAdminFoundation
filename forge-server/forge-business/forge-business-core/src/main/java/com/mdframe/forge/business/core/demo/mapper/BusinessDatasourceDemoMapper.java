package com.mdframe.forge.business.core.demo.mapper;

import com.mdframe.forge.business.core.demo.domain.BusinessDatasourceDemoRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 租户业务数据源路由演示 Mapper。
 */
@Mapper
public interface BusinessDatasourceDemoMapper {

    void createTable();

    void insertRecord(@Param("record") BusinessDatasourceDemoRecord record);

    List<BusinessDatasourceDemoRecord> selectRecent(@Param("tenantId") Long tenantId, @Param("limit") Integer limit);

    String selectDatabaseName();
}
