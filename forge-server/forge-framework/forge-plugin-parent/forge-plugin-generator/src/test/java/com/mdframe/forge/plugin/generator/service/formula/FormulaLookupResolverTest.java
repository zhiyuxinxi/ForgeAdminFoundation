package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.LookupConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FormulaLookupResolver")
@Tag("dev")
class FormulaLookupResolverTest {

    private LookupConfig lookup() {
        return new LookupConfig(
                "customer_owner",
                "sys_user",
                "ownerUserId",
                "id",
                "realName",
                "NOT_ASSIGNED");
    }

    @Test
    @DisplayName("matched row returns configured return field")
    void matchedRowReturnsReturnField() {
        FormulaLookupResolver resolver = new FormulaLookupResolver((config, context) -> List.of(
                Map.of("id", "u1", "realName", "Alice"),
                Map.of("id", "u2", "realName", "Bob")));

        LookupResolveResult result = resolver.resolve(lookup(), Map.of("ownerUserId", "u2"));

        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertTrue(result.isMatched());
        assertEquals("Bob", result.getValue());
        assertEquals("customer_owner", result.getMetadata().get("relationCode"));
        assertEquals(1, result.getMetadata().get("matchedRowCount"));
    }

    @Test
    @DisplayName("not found returns notFoundValue")
    void notFoundReturnsFallback() {
        FormulaLookupResolver resolver = new FormulaLookupResolver((config, context) -> List.of(
                Map.of("id", "u1", "realName", "Alice")));

        LookupResolveResult result = resolver.resolve(lookup(), Map.of("ownerUserId", "u9"));

        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertFalse(result.isMatched());
        assertEquals("NOT_ASSIGNED", result.getValue());
        assertEquals(0, result.getMetadata().get("matchedRowCount"));
    }

    @Test
    @DisplayName("null source value returns notFoundValue")
    void nullSourceReturnsFallback() {
        FormulaLookupResolver resolver = new FormulaLookupResolver((config, context) -> List.of(
                Map.of("id", "u1", "realName", "Alice")));

        LookupResolveResult result = resolver.resolve(lookup(), Map.of("customerName", "A"));

        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertFalse(result.isMatched());
        assertEquals("NOT_ASSIGNED", result.getValue());
    }

    @Test
    @DisplayName("snake case target fields can match camel case config")
    void snakeCaseTargetFieldsCanMatchCamelCaseConfig() {
        FormulaLookupResolver resolver = new FormulaLookupResolver((config, context) -> List.of(
                Map.of("id", "u1", "real_name", "Alice")));

        LookupResolveResult result = resolver.resolve(lookup(), Map.of("owner_user_id", "u1"));

        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertTrue(result.isMatched());
        assertEquals("Alice", result.getValue());
    }

    @Test
    @DisplayName("provider exception is returned as resolver failure")
    void providerExceptionReturnsFailure() {
        FormulaLookupResolver resolver = new FormulaLookupResolver((config, context) -> {
            throw new IllegalStateException("relation path invalid");
        });

        LookupResolveResult result = resolver.resolve(lookup(), Map.of("ownerUserId", "u1"));

        assertFalse(result.isSuccess());
        assertEquals("relation path invalid", result.getErrorMessage());
        assertEquals("customer_owner", result.getMetadata().get("relationCode"));
    }
}
