package com.mdframe.forge.flow.bridge;

import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Standalone flow server only needs generator runtime services for dynamic CRUD writes.
 * Menu registration belongs to the admin server, so keep it as a no-op here.
 */
@Slf4j
@Component
public class FlowMenuRegisterAdapter implements MenuRegisterAdapter {

    @Override
    public Long registerMenu(String menuName, Long parentId, String configKey, Integer sort) {
        log.warn("[FlowMenuRegisterAdapter] Skip menu registration in flow server: menuName={}, configKey={}",
                menuName, configKey);
        return null;
    }

    @Override
    public void updateMenu(Long menuResourceId, String menuName, Long parentId, Integer sort) {
        log.warn("[FlowMenuRegisterAdapter] Skip menu update in flow server: menuId={}, menuName={}",
                menuResourceId, menuName);
    }

    @Override
    public void deleteMenu(Long menuResourceId) {
        log.warn("[FlowMenuRegisterAdapter] Skip menu delete in flow server: menuId={}", menuResourceId);
    }
}
