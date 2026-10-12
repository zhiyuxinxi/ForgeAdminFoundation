package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.FormulaDependencyAnalyzer;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaDependencyGraphRequest;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaDependencyGraphResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FormulaDependencyGraphService")
@Tag("dev")
class FormulaDependencyGraphServiceTest {

    private final FormulaDependencyGraphService service =
            new FormulaDependencyGraphService(new FormulaDependencyAnalyzer());

    @Test
    @DisplayName("A depends on B and C returns 3 nodes and 2 edges")
    void simpleDependencyGraph() {
        FormulaDependencyGraphRequest request = new FormulaDependencyGraphRequest();
        request.setObjectCode("order");
        request.setFormulas(List.of(calc("A", "B + C", List.of("B", "C"))));

        FormulaDependencyGraphResponse response = service.graph(request);

        assertTrue(response.isValid(), "Errors: " + response.getErrors());
        assertFalse(response.isHasCycle());
        assertEquals(3, response.getNodes().size());
        assertEquals(2, response.getEdges().size());
        assertTrue(response.getNodes().stream().anyMatch(node -> "A".equals(node.getId())
                && "FORMULA".equals(node.getType())));
        assertTrue(response.getEdges().stream().allMatch(edge -> "DEPENDS_ON".equals(edge.getType())));
    }

    @Test
    @DisplayName("cycle returns cycle path")
    void cycleReturnsCyclePath() {
        FormulaDependencyGraphRequest request = new FormulaDependencyGraphRequest();
        request.setFormulas(List.of(
                calc("A", "B + 1", List.of("B")),
                calc("B", "A + 1", List.of("A"))
        ));

        FormulaDependencyGraphResponse response = service.graph(request);

        assertFalse(response.isValid());
        assertTrue(response.isHasCycle());
        assertFalse(response.getCyclePath().isEmpty());
    }

    @Test
    @DisplayName("aggregate formula returns relation node and aggregate edge")
    void aggregateReturnsRelationEdge() {
        FormulaDependencyGraphRequest request = new FormulaDependencyGraphRequest();
        FormulaDependencyGraphRequest.FormulaFieldConfig formula = new FormulaDependencyGraphRequest.FormulaFieldConfig();
        formula.setFieldCode("totalAmount");
        formula.setType("AGGREGATE");
        FormulaDependencyGraphRequest.AggregateGraph aggregate = new FormulaDependencyGraphRequest.AggregateGraph();
        aggregate.setFunction("SUM");
        aggregate.setRelationCode("order_items");
        aggregate.setTargetField("amount");
        formula.setAggregate(aggregate);
        request.setFormulas(List.of(formula));

        FormulaDependencyGraphResponse response = service.graph(request);

        assertTrue(response.isValid(), "Errors: " + response.getErrors());
        assertEquals(2, response.getNodes().size());
        assertEquals(1, response.getEdges().size());
        assertEquals("AGGREGATE", response.getEdges().get(0).getType());
        assertTrue(response.getNodes().stream().anyMatch(node -> "RELATION".equals(node.getType())));
    }

    @Test
    @DisplayName("lookup formula returns dependency and lookup edges")
    void lookupReturnsLookupEdge() {
        FormulaDependencyGraphRequest request = new FormulaDependencyGraphRequest();
        request.setObjectCode("customer");
        FormulaDependencyGraphRequest.FormulaFieldConfig formula = new FormulaDependencyGraphRequest.FormulaFieldConfig();
        formula.setFieldCode("ownerName");
        formula.setType("LOOKUP");
        formula.setDependsOn(List.of("ownerUserId"));
        FormulaDependencyGraphRequest.LookupGraph lookup = new FormulaDependencyGraphRequest.LookupGraph();
        lookup.setRelationCode("customer_owner");
        lookup.setTargetObjectCode("sys_user");
        lookup.setSourceField("ownerUserId");
        lookup.setTargetField("id");
        lookup.setReturnField("realName");
        formula.setLookup(lookup);
        request.setFormulas(List.of(formula));

        FormulaDependencyGraphResponse response = service.graph(request);

        assertTrue(response.isValid(), "Errors: " + response.getErrors());
        assertTrue(response.getEdges().stream().anyMatch(edge -> "DEPENDS_ON".equals(edge.getType())));
        assertTrue(response.getEdges().stream().anyMatch(edge -> "LOOKUP".equals(edge.getType())));
        assertTrue(response.getNodes().stream().anyMatch(node -> "RELATION".equals(node.getType())));
    }

    @Test
    @DisplayName("cross-object formula returns cross object edge")
    void crossObjectReturnsCrossObjectEdge() {
        FormulaDependencyGraphRequest request = new FormulaDependencyGraphRequest();
        request.setObjectCode("order");
        FormulaDependencyGraphRequest.FormulaFieldConfig formula = new FormulaDependencyGraphRequest.FormulaFieldConfig();
        formula.setFieldCode("customerLevel");
        formula.setType("CALC");
        formula.setExpression("customer.level");
        formula.setDependsOn(List.of("customerId"));
        FormulaDependencyGraphRequest.CrossObjectGraph crossObject = new FormulaDependencyGraphRequest.CrossObjectGraph();
        crossObject.setPath("customer.level");
        crossObject.setRelationCode("customer");
        crossObject.setTargetObjectCode("crm_customer");
        crossObject.setReturnField("level");
        formula.setCrossObject(crossObject);
        request.setFormulas(List.of(formula));

        FormulaDependencyGraphResponse response = service.graph(request);

        assertTrue(response.isValid(), "Errors: " + response.getErrors());
        assertTrue(response.getEdges().stream().anyMatch(edge -> "CROSS_OBJECT".equals(edge.getType())));
        assertTrue(response.getNodes().stream().anyMatch(node -> "RELATION".equals(node.getType())));
    }

    private FormulaDependencyGraphRequest.FormulaFieldConfig calc(String fieldCode,
                                                                  String expression,
                                                                  List<String> dependsOn) {
        FormulaDependencyGraphRequest.FormulaFieldConfig formula = new FormulaDependencyGraphRequest.FormulaFieldConfig();
        formula.setFieldCode(fieldCode);
        formula.setType("CALC");
        formula.setExpression(expression);
        formula.setDependsOn(dependsOn);
        return formula;
    }
}
