package com.mdframe.forge.plugin.generator.service.formula;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaFunction;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaFunctionInstall;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaFunctionVersion;
import com.mdframe.forge.plugin.generator.mapper.FormulaFunctionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("FormulaFunctionPersistence")
@Tag("dev")
class FormulaFunctionPersistenceTest {

    @Test
    @DisplayName("entity fields hold function metadata")
    void entityFields() {
        AiFormulaFunction function = new AiFormulaFunction();
        function.setTenantId(1L);
        function.setFunctionCode("math.round");
        function.setDisplayName("四舍五入");
        function.setCategory("Math");
        function.setSourceType("BUILTIN");
        function.setReturnType("NUMBER");
        function.setStatus("ENABLED");

        AiFormulaFunctionVersion version = new AiFormulaFunctionVersion();
        version.setFunctionCode(function.getFunctionCode());
        version.setVersion("1.0.0");
        version.setImplementationType("JAVA_BEAN");
        version.setBeanName("formulaBuiltinFunctionProvider");
        version.setMethodName("mathRound");

        AiFormulaFunctionInstall install = new AiFormulaFunctionInstall();
        install.setTenantId(1L);
        install.setFunctionCode(function.getFunctionCode());
        install.setInstalledVersion(version.getVersion());
        install.setEnabled(true);

        assertEquals("math.round", function.getFunctionCode());
        assertEquals("JAVA_BEAN", version.getImplementationType());
        assertEquals(Boolean.TRUE, install.getEnabled());
    }

    @Test
    @DisplayName("mapper exposes page detail version and install queries")
    void mapperMethods() throws Exception {
        Method page = FormulaFunctionMapper.class.getMethod(
            "selectFormulaFunctionPage",
            Page.class,
            Long.class,
            String.class,
            String.class,
            String.class,
            String.class);
        Method detail = FormulaFunctionMapper.class.getMethod(
            "selectFormulaFunctionByCode",
            Long.class,
            String.class);
        Method versions = FormulaFunctionMapper.class.getMethod(
            "selectFunctionVersions",
            Long.class,
            String.class);
        Method install = FormulaFunctionMapper.class.getMethod(
            "selectFunctionInstall",
            Long.class,
            String.class);

        assertNotNull(page);
        assertEquals(AiFormulaFunction.class, detail.getReturnType());
        assertEquals(List.class, versions.getReturnType());
        assertEquals(AiFormulaFunctionInstall.class, install.getReturnType());
    }
}
