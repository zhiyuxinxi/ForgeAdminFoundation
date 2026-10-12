package com.mdframe.forge.plugin.system.listener;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

public class DictChangeEvent extends ApplicationEvent {

    private final Set<String> dictTypes;

    public DictChangeEvent(Object source, Set<String> dictTypes) {
        super(source);
        this.dictTypes = dictTypes;
    }

    public DictChangeEvent(Object source, String dictType) {
        super(source);
        this.dictTypes = Set.of(dictType);
    }

    public Set<String> getDictTypes() {
        return dictTypes;
    }
}
