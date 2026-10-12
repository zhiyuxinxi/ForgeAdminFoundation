package com.mdframe.forge.plugin.generator.service.formula;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FormulaValueMasker")
@Tag("dev")
class FormulaValueMaskerTest {

    private final FormulaValueMasker masker = new FormulaValueMasker();

    @Test
    @DisplayName("masks sensitive JSON keys")
    void masksSensitiveJsonKeys() {
        String masked = masker.mask("{\"mobile\":\"13812345678\",\"token\":\"abc123\",\"amount\":100}");

        assertEquals("{\"mobile\":\"****\",\"token\":\"****\",\"amount\":100}", masked);
    }

    @Test
    @DisplayName("masks standalone phone id card and bank card values")
    void masksStandaloneSensitiveValues() {
        String masked = masker.mask("phone=13812345678 id=110101199001011234 card=6222021234567890123");

        assertTrue(masked.contains("138****5678"));
        assertTrue(masked.contains("1101011990******1234"));
        assertTrue(masked.contains("622202****0123"));
    }
}
