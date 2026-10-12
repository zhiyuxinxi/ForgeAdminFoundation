package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.lowcode.formula.runtime.FormulaExecution;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

/** Compatibility constructors and Spring wiring for the independent formula runtime. */
@Component
public class FormulaExecutionEngine {
    private final FormulaExecution runtime;

    /**
     * Preferred constructor - requires an AggregateDataProvider for AGGREGATE formula support.
     */
    public FormulaExecutionEngine(AggregateDataProvider dataProvider) {
        this(dataProvider, FormulaLookupResolver.unsupported());
    }

    public FormulaExecutionEngine(AggregateDataProvider dataProvider,
                                  FormulaLookupResolver lookupResolver) {
        this(dataProvider, lookupResolver, FormulaFunctionRegistry.builtin());
    }

    public FormulaExecutionEngine(AggregateDataProvider dataProvider,
                                  FormulaLookupResolver lookupResolver,
                                  FormulaFunctionRegistry functionRegistry) {
        this(dataProvider, lookupResolver, functionRegistry, null);
    }

    @Autowired
    public FormulaExecutionEngine(AggregateDataProvider dataProvider,
                                  FormulaLookupResolver lookupResolver,
                                  FormulaFunctionRegistry functionRegistry,
                                  FormulaFunctionMarketService functionMarketService) {
        this(new ExpressionExecutor(new AviatorAdapter(), functionRegistry), new FormulaDependencyAnalyzer(),
             new FormulaErrorHandler(),
             new AggregateEngine(new AggregateFunctionExecutor(),
                 new ExpressionExecutor(new AviatorAdapter(), functionRegistry), dataProvider),
             lookupResolver,
             functionMarketService);
    }

    public FormulaExecutionEngine(ExpressionExecutor executor,
                                   FormulaDependencyAnalyzer dependencyAnalyzer,
                                   FormulaErrorHandler errorHandler,
                                   AggregateEngine aggregateEngine) {
        this(executor, dependencyAnalyzer, errorHandler, aggregateEngine,
            FormulaLookupResolver.unsupported());
    }

    public FormulaExecutionEngine(ExpressionExecutor executor,
                                   FormulaDependencyAnalyzer dependencyAnalyzer,
                                   FormulaErrorHandler errorHandler,
                                   AggregateEngine aggregateEngine,
                                   FormulaLookupResolver lookupResolver) {
        this(executor, dependencyAnalyzer, errorHandler, aggregateEngine, lookupResolver, null);
    }

    public FormulaExecutionEngine(ExpressionExecutor executor,
                                   FormulaDependencyAnalyzer dependencyAnalyzer,
                                   FormulaErrorHandler errorHandler,
                                   AggregateEngine aggregateEngine,
                                   FormulaLookupResolver lookupResolver,
                                   FormulaFunctionMarketService functionMarketService) {
        this.runtime = new FormulaExecution(executor, dependencyAnalyzer, errorHandler,
                aggregateEngine, lookupResolver, functionMarketService);
    }

    @Deprecated
    public FormulaExecutionEngine() {
        this(new ExpressionExecutor(), new FormulaDependencyAnalyzer(),
             new FormulaErrorHandler(), new AggregateEngine());
    }

    public ExecutionResult execute(Map<String, FormulaConfig> formulaMap, Map<String, Object> originalValues) {
        return runtime.execute(formulaMap, originalValues);
    }

    public ExecutionResult execute(Map<String, FormulaConfig> formulaMap, Map<String, Object> originalValues,
                                   FormulaTraceOptions traceOptions) {
        return runtime.execute(formulaMap, originalValues, traceOptions);
    }

    FormulaExecution runtime() {
        return runtime;
    }
}
