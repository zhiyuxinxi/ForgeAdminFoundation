package com.mdframe.forge.plugin.generator.service.lowcode;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.lowcode.api.datasource.RuntimeDataSourceAccess;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeDatasourceSnapshot;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.plugin.generator.support.GeneratorJdbcDataSources;
import com.mdframe.forge.plugin.generator.util.GenDatasourcePasswordCodec;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Owns design persistence, cached encrypted definitions and access to existing JDBC pools. */
@Component
@RequiredArgsConstructor
public class GeneratorRuntimeDataSourceAccess implements RuntimeDataSourceAccess {
    private static final long DATASOURCE_CACHE_TTL_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static final int DATASOURCE_CACHE_MAX_SIZE = 512;
    private static final String DEFAULT_DB_TYPE = "MySQL";
    private final GenDatasourceMapper datasourceMapper;
    private final GeneratorJdbcDataSources generatorJdbcDataSources;
    private final Map<Long, CacheEntry<GenDatasource>> datasourceByIdCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<GenDatasource>> datasourceByCodeCache = new ConcurrentHashMap<>();

    @Override
    public LowcodeRuntimeDatasourceSnapshot resolveSnapshot(Long datasourceId, String datasourceCode,
                                                            String tableName, String tableMode) {
        GenDatasource datasource = datasourceId != null ? getCachedDatasourceById(datasourceId)
            : getCachedDatasourceByCode(datasourceCode);
        if (datasource == null) {
            throw new BusinessException(UiMessages.translate("运行数据源不存在: ") + StringUtils.firstNonBlank(
                datasourceCode, datasourceId == null ? null : String.valueOf(datasourceId)));
        }
        assertEnabled(datasource);
        return buildSnapshot(datasource, tableName, tableMode);
    }

    @Override
    public DataSource dataSource(Long datasourceId) {
        if (datasourceId == null) {
            throw new BusinessException("运行数据源ID不能为空");
        }
        GenDatasource datasource = getDatasourceById(datasourceId);
        if (datasource == null) {
            throw new BusinessException(UiMessages.translate("运行数据源不存在: ") + datasourceId);
        }
        assertEnabled(datasource);
        datasource.setPassword(GenDatasourcePasswordCodec.decrypt(datasource.getPassword()));
        return generatorJdbcDataSources.getDataSource(datasource);
    }

    private void assertEnabled(GenDatasource datasource) {
        if (Integer.valueOf(0).equals(datasource.getIsEnabled())) {
            throw new BusinessException(UiMessages.translate("运行数据源已禁用: ") + datasource.getDatasourceName());
        }
    }

    public LowcodeRuntimeDatasourceSnapshot buildSnapshot(GenDatasource datasource, String tableName, String tableMode) {
        if (datasource == null) {
            return null;
        }
        LowcodeRuntimeDatasourceSnapshot snapshot = new LowcodeRuntimeDatasourceSnapshot();
        snapshot.setDatasourceId(datasource.getDatasourceId());
        snapshot.setDatasourceCode(datasource.getDatasourceCode());
        snapshot.setDatasourceName(datasource.getDatasourceName());
        snapshot.setDbType(StringUtils.defaultIfBlank(datasource.getDbType(), DEFAULT_DB_TYPE));
        snapshot.setTableName(tableName);
        snapshot.setTableMode(tableMode);
        snapshot.setUsageScope(datasource.getUsageScope());
        snapshot.setReadonly(Integer.valueOf(1).equals(datasource.getReadonly()));
        snapshot.setAllowWrite(!Boolean.TRUE.equals(snapshot.getReadonly())
            && Integer.valueOf(1).equals(datasource.getAllowRuntimeWrite()));
        snapshot.setAllowDdl(!Boolean.TRUE.equals(snapshot.getReadonly())
            && Integer.valueOf(1).equals(datasource.getAllowRuntimeDdl()));
        snapshot.setRiskLevel(datasource.getRiskLevel());
        return snapshot;
    }

    public GenDatasource getDatasourceById(Long datasourceId) {
        if (datasourceId == null) {
            return null;
        }
        return getCachedDatasourceById(datasourceId);
    }

    public void evictDatasource(Long datasourceId, String datasourceCode) {
        if (datasourceId != null) {
            datasourceByIdCache.remove(datasourceId);
        }
        String codeKey = normalizeDatasourceCode(datasourceCode);
        if (StringUtils.isNotBlank(codeKey)) {
            datasourceByCodeCache.remove(codeKey);
        }
    }

    public void clearDatasourceCache() {
        datasourceByIdCache.clear();
        datasourceByCodeCache.clear();
    }

    private GenDatasource getCachedDatasourceById(Long datasourceId) {
        long now = System.currentTimeMillis();
        CacheEntry<GenDatasource> cached = datasourceByIdCache.get(datasourceId);
        if (cached != null && cached.expiresAt() > now) {
            return copyDatasource(cached.value());
        }
        GenDatasource datasource = datasourceMapper.selectById(datasourceId);
        cacheDatasource(datasource, now);
        return copyDatasource(datasource);
    }

    private GenDatasource getCachedDatasourceByCode(String datasourceCode) {
        String codeKey = normalizeDatasourceCode(datasourceCode);
        if (StringUtils.isBlank(codeKey)) {
            return null;
        }
        long now = System.currentTimeMillis();
        CacheEntry<GenDatasource> cached = datasourceByCodeCache.get(codeKey);
        if (cached != null && cached.expiresAt() > now) {
            return copyDatasource(cached.value());
        }
        GenDatasource datasource = datasourceMapper.selectOne(new LambdaQueryWrapper<GenDatasource>()
            .eq(GenDatasource::getDatasourceCode, datasourceCode)
            .last("LIMIT 1"));
        cacheDatasource(datasource, now);
        return copyDatasource(datasource);
    }

    private void cacheDatasource(GenDatasource datasource, long now) {
        if (datasource == null) {
            return;
        }
        trimExpiredCacheIfNeeded(now);
        CacheEntry<GenDatasource> entry = new CacheEntry<>(
            copyDatasource(datasource), now + DATASOURCE_CACHE_TTL_MILLIS);
        if (datasource.getDatasourceId() != null) {
            datasourceByIdCache.put(datasource.getDatasourceId(), entry);
        }
        String codeKey = normalizeDatasourceCode(datasource.getDatasourceCode());
        if (StringUtils.isNotBlank(codeKey)) {
            datasourceByCodeCache.put(codeKey, entry);
        }
    }

    private void trimExpiredCacheIfNeeded(long now) {
        if (datasourceByIdCache.size() > DATASOURCE_CACHE_MAX_SIZE) {
            datasourceByIdCache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        }
        if (datasourceByCodeCache.size() > DATASOURCE_CACHE_MAX_SIZE) {
            datasourceByCodeCache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        }
    }

    private GenDatasource copyDatasource(GenDatasource datasource) {
        return datasource == null ? null : BeanUtil.copyProperties(datasource, GenDatasource.class);
    }

    private String normalizeDatasourceCode(String datasourceCode) {
        return StringUtils.trimToEmpty(datasourceCode);
    }

    private record CacheEntry<T>(T value, long expiresAt) {
    }
}
