package com.mdframe.forge.starter.datascope.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据权限范围枚举
 */
@Getter
@AllArgsConstructor
public enum DataScopeType {
    
    /**
     * 全部数据权限
     */
    ALL(1, "全部数据权限"),
    
    /**
     * 本人数据权限
     */
    SELF(2, "本人数据权限"),
    
    /**
     * 本组织数据权限
     */
    ORG(3, "本组织数据权限"),
    
    /**
     * 本组织及子组织数据权限
     */
    ORG_AND_CHILD(4, "本组织及子组织数据权限"),
    
    /**
     * 自定义数据权限
     */
    CUSTOM(5, "自定义数据权限"),
    
    /**
     * 租户全部数据权限
     */
    TENANT_ALL(6, "租户全部数据权限"),

    /**
     * 本行政区划数据权限
     * 按用户所属行政区划（regionCode）过滤：
     * - 省级(level=1)：不限制（等同 ALL）
     * - 市级及以下：匹配本级编码 + 其直接下级区划（由平台主库快照提前解析）
     * 支持可选的用户表 area_code 联合 OR 条件
     */
    REGION(7, "本行政区划数据权限");
    
    private final Integer code;
    private final String description;
    
    /**
     * 根据code获取枚举
     */
    public static DataScopeType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataScopeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 兼容角色数据范围的两套历史编码：
     * 当前字典：1全部、2本租户、3本组织、4本组织及子组织、5个人、7行政区划；
     * 旧后端枚举：1全部、2个人、3本组织、4本组织及子组织、5自定义、6本租户、7行政区划。
     */
    public static DataScopeType getByRoleDataScope(Integer code, boolean hasCustomOrgIds) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 1 -> ALL;
            case 2 -> TENANT_ALL;
            case 3 -> ORG;
            case 4 -> ORG_AND_CHILD;
            case 5 -> hasCustomOrgIds ? CUSTOM : SELF;
            case 6 -> TENANT_ALL;
            case 7 -> REGION;
            default -> getByCode(code);
        };
    }
}
