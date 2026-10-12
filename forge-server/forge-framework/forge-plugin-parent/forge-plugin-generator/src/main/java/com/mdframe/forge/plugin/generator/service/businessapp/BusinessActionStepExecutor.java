package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionStepDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessActionStepResultVO;

public interface BusinessActionStepExecutor {

    String supportType();

    BusinessActionStepResultVO execute(BusinessActionExecutionContext context, BusinessActionStepDTO step);
}
