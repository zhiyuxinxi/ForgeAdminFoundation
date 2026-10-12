package com.mdframe.forge.plugin.generator.dto.lowcode;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 低代码页面搭建区域。
 */
@Data
public class LowcodePageZone {

    /** search/table/edit/detail/toolbar */
    @JsonAlias("key")
    private String zoneKey;

    @JsonAlias("type")
    private String componentKey;

    private Boolean enabled;

    private List<String> fieldRefs = new ArrayList<>();

    private Map<String, Object> props = new LinkedHashMap<>();
}
