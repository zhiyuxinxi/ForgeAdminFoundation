package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.runtime.child.RuntimeFieldRef;
import com.mdframe.forge.plugin.generator.service.runtime.child.RuntimeChildRelation;
import java.util.List;
import java.util.Map;

/** Compiled relation metadata shared by existing query and write consumers. */
public record RuntimeJoinContext(Map<String, RuntimeFieldRef> fields,
                                      List<RuntimeChildRelation> childRelations,
                                      List<DynamicCrudRepository.JoinField> selectFields,
                                      List<DynamicCrudRepository.JoinSpec> joins,
                                      Map<String, String> fieldColumnMapping,
                                      Map<String, String> relationDisplayAliases,
                                      Map<String, String> primaryColumnMapping) {
    }
