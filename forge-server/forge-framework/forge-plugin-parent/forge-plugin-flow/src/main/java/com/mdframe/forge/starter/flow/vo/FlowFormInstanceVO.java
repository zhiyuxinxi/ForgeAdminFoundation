package com.mdframe.forge.starter.flow.vo;

import com.mdframe.forge.starter.flow.entity.FlowFormInstance;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程表单实例详情。
 */
@Data
public class FlowFormInstanceVO {

    private FlowFormInstance instance;

    private List<Map<String, Object>> history;

    private Object diagram;
}
