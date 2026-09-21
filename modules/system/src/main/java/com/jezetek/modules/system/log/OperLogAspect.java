package com.jezetek.modules.system.log;

import com.jezetek.core.runtime.log.Log;
import com.jezetek.core.runtime.security.LoginUser;
import com.jezetek.core.runtime.security.SecurityUtils;
import com.jezetek.modules.system.model.OperLog;
import com.jezetek.modules.system.model.OperLogDraft;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

/**
 * 操作日志切面：环绕 @Log 标注的写操作，采集
 * 操作人/URI/入参/结果/耗时/异常后在后台线程落库。
 *
 * <p>实体在调用线程组装完成(SecurityContext/RequestContext 仅调用线程可见)，
 * 仅 DB 写入交给 operLogExecutor 异步执行</p>
 */
@Aspect
@Component
public class OperLogAspect {

    private static final int MAX_TEXT_LENGTH = 2000;

    private static final Pattern PASSWORD_MASK = Pattern.compile("(password=)[^,)]*");

    private final OperLogWriter operLogWriter;

    private final Executor operLogExecutor;

    private final ObjectMapper objectMapper;

    public OperLogAspect(
            OperLogWriter operLogWriter,
            @Qualifier("operLogExecutor") Executor operLogExecutor,
            ObjectMapper objectMapper
    ) {
        this.operLogWriter = operLogWriter;
        this.operLogExecutor = operLogExecutor;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint joinPoint, Log log) throws Throwable {
        long start = System.currentTimeMillis();
        String uri = currentUri();
        String operator = currentOperator();
        String params = collectParams(joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            long costMs = System.currentTimeMillis() - start;
            String resultJson = toJson(result);
            submit(log, operator, uri, params, resultJson, null, costMs, true);
            return result;
        } catch (Throwable e) {
            long costMs = System.currentTimeMillis() - start;
            submit(log, operator, uri, params, null, truncate(e.toString()), costMs, false);
            throw e;
        }
    }

    private void submit(
            Log log,
            String operator,
            String uri,
            String params,
            String result,
            String errorMsg,
            long costMs,
            boolean success
    ) {
        OperLog entity = OperLogDraft.$.produce(draft -> {
            draft.setModule(log.module());
            draft.setAction(log.action());
            draft.setOperator(operator);
            draft.setUri(uri);
            draft.setParams(params);
            draft.setResult(result);
            draft.setErrorMsg(errorMsg);
            draft.setCostMs(costMs);
            draft.setSuccess(success);
        });
        operLogExecutor.execute(() -> {
            try {
                operLogWriter.write(entity);
            } catch (Exception ex) {
                // 日志落库失败不影响业务，也不形成递归
            }
        });
    }

    private static String currentOperator() {
        LoginUser user = SecurityUtils.currentLoginUser();
        return user != null ? user.username() : "anonymous";
    }

    private static String currentUri() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRequestURI();
        }
        return null;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            return "unserializable: " + value.getClass().getSimpleName();
        }
    }

    /**
     * 入参用 toString 采集而非 JSON 序列化：jimmer 生成 Input 的集合 getter
     * 会懒初始化空列表(见 AGENTS.md Jimmer 要点)，JSON 序列化调用 getter 会把
     * "未提交"污染成"已提交空列表"，导致后续 isProvided 误判而清空关联。
     * 生成 Input 的 toString 直接读字段，无副作用。密码统一脱敏
     */
    private static String collectParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        String text = Arrays.toString(args);
        return truncate(PASSWORD_MASK.matcher(text).replaceAll("$1***"));
    }

    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TEXT_LENGTH) + "...(truncated)";
    }
}
