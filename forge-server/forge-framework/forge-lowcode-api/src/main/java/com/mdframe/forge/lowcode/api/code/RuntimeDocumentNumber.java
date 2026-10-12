package com.mdframe.forge.lowcode.api.code;

/** One selected document-number definition; raw rules retain the generator's parsing semantics. */
public record RuntimeDocumentNumber(Long tenantId,
                                    String suiteCode,
                                    String objectCode,
                                    Integer documentEnabled,
                                    String rawNoRule,
                                    String documentNoField) {
}
