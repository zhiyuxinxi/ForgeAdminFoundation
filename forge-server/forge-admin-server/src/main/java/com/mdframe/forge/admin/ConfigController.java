package com.mdframe.forge.admin;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.context.LogProperties;
import com.mdframe.forge.starter.crypto.desensitize.annotation.Desensitize;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategy;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import com.mdframe.forge.starter.property.example.AppConfigExample;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ConfigController {

    @Autowired
    private AppConfigExample appConfigExample;
    
    @Autowired
    private LogProperties logProperties;
    
    @Autowired
    private Environment environment;
    
    @GetMapping("/config")
    @SaIgnore
    public Object getConfig() {
        return environment.getProperty("forge.log.print-operation-log") + ".." +
                logProperties.getPrintOperationLog() + "enableOperationLog:" + logProperties.getEnableOperationLog();
    }
    
    @GetMapping("/desensitize")
    @SaIgnore
    @ApiPermissionIgnore
    public List<TestBean> desensitize() {
        List<TestBean> testBeans = new ArrayList<>();
        TestBean testBean = new TestBean();
        testBean.setName("测试");
        testBean.setPhone("18647122213");
        testBeans.add(testBean);
        return testBeans;
    }
    
    @Data
    public static class TestBean {
        
        private String name;
        
        @Desensitize(type = DesensitizeType.PHONE)
        private String phone;
    }
}
