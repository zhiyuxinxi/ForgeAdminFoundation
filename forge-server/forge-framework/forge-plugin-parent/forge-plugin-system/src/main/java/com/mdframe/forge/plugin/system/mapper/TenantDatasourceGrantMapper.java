package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.mdframe.forge.plugin.system.datasource.TenantDatasourceOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@DS("master")
public interface TenantDatasourceGrantMapper {
    List<TenantDatasourceOption> selectOptions(@Param("tenantId") Long tenantId);

    TenantDatasourceOption selectDatasource(@Param("tenantId") Long tenantId,
                                            @Param("datasourceId") Long datasourceId);

    int grant(@Param("tenantId") Long tenantId, @Param("datasourceId") Long datasourceId,
              @Param("userId") Long userId);

    int revoke(@Param("tenantId") Long tenantId, @Param("datasourceId") Long datasourceId);
}
