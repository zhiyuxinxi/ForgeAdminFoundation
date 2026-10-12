package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.ISysDictDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DictionaryLocaleTest {
    @AfterEach void reset() { LocaleContextHolder.resetLocaleContext(); }

    @Test void sharedDictionaryDataSupportsBothLanguagesWithoutChangingKeys() {
        ISysDictDataService data = mock(ISysDictDataService.class);
        var item = new SysDictData();
        item.setDictLabel("待审批"); item.setDictLabelEn("Pending approval"); item.setDictValue("PENDING");
        when(data.selectDictDataByType("status")).thenReturn(List.of(item));
        var provider = new SytemDictValueProvider(data, mock(SysOrgMapper.class), mock(SysUserMapper.class),
                mock(SysRegionMapper.class), mock(SysFileMetadataMapper.class));
        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("Pending approval", provider.getLabel("status", "PENDING"));
        assertEquals("PENDING", provider.getValue("status", "Pending approval"));
        assertEquals("PENDING", provider.getValue("status", "待审批"));
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("待审批", provider.getLabel("status", "PENDING"));
        assertEquals("待审批", item.getDictLabel());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test void legacyMapCacheKeepsEnglishMetadata() {
        ISysDictDataService data = mock(ISysDictDataService.class);
        when(data.selectDictDataByType("status")).thenReturn((List) List.of(Map.of(
                "dict_value", "A", "dict_label", "启用", "dict_label_en", "Enabled")));
        var provider = new SytemDictValueProvider(data, mock(SysOrgMapper.class), mock(SysUserMapper.class),
                mock(SysRegionMapper.class), mock(SysFileMetadataMapper.class));
        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("Enabled", provider.getLabel("status", "A"));
    }
}
