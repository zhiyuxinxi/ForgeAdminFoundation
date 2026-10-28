package com.mdframe.forge.flow.client.helper;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowClientException;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowStart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link FlowStart} 注解 AOP 切面（业务侧）
 * <p>
 * 业务方法执行成功后，自动通过 {@link FlowClient} 发起远程流程，
 * 无需业务代码侵入。
 * <p>
 * 由 {@link FlowClientHelperAutoConfiguration} 自动注册，
 * 只要引入 {@code forge-flow-client} 并配置好 {@code forge.flow.client.url} 即可生效。
 *
 * @author forge
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class FlowStartAspect {

    private final FlowClient flowClient;
    private final ApplicationContext applicationContext;

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    @Around("@annotation(flowStart)")
    public Object around(ProceedingJoinPoint pjp, FlowStart flowStart) throws Throwable {
        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            if (!flowStart.skipOnError()) {
                log.warn("[FlowStart] 业务方法异常，尝试发起流程: {}", ex.getMessage());
                try {
                    tryStart(pjp, flowStart, null);
                } catch (RuntimeException startFailure) {
                    ex.addSuppressed(startFailure);
                }
            }
            throw ex;
        }
        tryStart(pjp, flowStart, result);
        return result;
    }

    private void tryStart(ProceedingJoinPoint pjp, FlowStart flowStart, Object result) {
        FlowBind bind = resolveFlowBind(pjp);
        String modelKey = StringUtils.hasText(flowStart.modelKey()) ? flowStart.modelKey()
                : bind == null ? null : bind.modelKey();
        EvaluationContext ctx = buildSpelContext(pjp, result);
        String businessKey = eval(flowStart.businessKeySpEl(), ctx, String.class);
        if (!StringUtils.hasText(modelKey) || !StringUtils.hasText(businessKey)) {
            throw new FlowClientException("FLOW_START_IDENTITY_REQUIRED");
        }
        String previousId = eval(flowStart.previousProcessInstanceIdSpEl(), ctx, String.class);
        if (StringUtils.hasText(flowStart.previousProcessInstanceIdSpEl()) && !StringUtils.hasText(previousId)) {
            throw new FlowClientException("FLOW_START_PREVIOUS_INSTANCE_REQUIRED");
        }
        String requestId = eval(flowStart.startRequestIdSpEl(), ctx, String.class);
        if (!StringUtils.hasText(requestId)) {
            if (StringUtils.hasText(flowStart.startRequestIdSpEl())) {
                throw new FlowClientException("FLOW_START_REQUEST_ID_REQUIRED");
            }
            requestId = UUID.nameUUIDFromBytes((modelKey + "\n" + businessKey + "\n"
                    + (previousId == null ? "" : previousId)).getBytes(StandardCharsets.UTF_8)).toString();
        }
        FlowStartInvocation request = new FlowStartInvocation(modelKey, businessKey, bind == null ? "" : bind.businessType(),
                eval(flowStart.titleSpEl(), ctx, String.class), readVariables(flowStart, ctx),
                eval(flowStart.userIdSpEl(), ctx, String.class), eval(flowStart.userNameSpEl(), ctx, String.class),
                eval(flowStart.deptIdSpEl(), ctx, String.class), eval(flowStart.deptNameSpEl(), ctx, String.class),
                requestId, previousId);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    request.execute(flowClient);
                }
            });
        } else {
            request.execute(flowClient);
        }
    }

    private Map<String, Object> readVariables(FlowStart flowStart, EvaluationContext ctx) {
        Map<String, Object> variables = new HashMap<>();
        if (StringUtils.hasText(flowStart.variablesSpEl())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> extra = eval(flowStart.variablesSpEl(), ctx, Map.class);
            if (extra != null) {
                variables.putAll(extra);
            }
        }
        return variables;
    }

    private FlowBind resolveFlowBind(ProceedingJoinPoint pjp) {
        Class<?> clazz = pjp.getTarget().getClass();
        FlowBind bind = clazz.getAnnotation(FlowBind.class);
        if (bind == null && clazz.getSuperclass() != null) {
            bind = clazz.getSuperclass().getAnnotation(FlowBind.class);
        }
        return bind;
    }

    private EvaluationContext buildSpelContext(ProceedingJoinPoint pjp, Object result) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        Parameter[] params = ((MethodSignature) pjp.getSignature()).getMethod().getParameters();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < params.length; i++) {
            ctx.setVariable(params[i].getName(), args[i]);
            ctx.setVariable("p" + i, args[i]);
        }
        if (result != null) {
            ctx.setVariable("result", result);
        }
        ctx.setBeanResolver((context, beanName) -> applicationContext.getBean(beanName));
        return ctx;
    }

    private <T> T eval(String spel, EvaluationContext ctx, Class<T> type) {
        if (!StringUtils.hasText(spel)) {
            return null;
        }
        try {
            return PARSER.parseExpression(spel).getValue(ctx, type);
        } catch (Exception e) {
            log.warn("[FlowStart] SpEL 解析失败 '{}': {}", spel, e.getMessage());
            return null;
        }
    }
}
