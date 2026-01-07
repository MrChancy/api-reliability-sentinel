package com.fluffycat.sentinelapp.common.aop;

import com.fluffycat.sentinelapp.common.trace.TraceIdUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ScheduleTraceAspect {

    public static final String JOB_NAME = "job";

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object withTraceId(ProceedingJoinPoint pjp) throws Throwable {
        String prev = MDC.get(TraceIdUtil.SCAN_ID);
        TraceIdUtil.getOrCreate(TraceIdUtil.SCAN_ID);
        MDC.put(JOB_NAME, pjp.getSignature().getDeclaringType().getSimpleName());
        try {
            return pjp.proceed();
        } finally {
            if (prev != null) MDC.put(TraceIdUtil.SCAN_ID, prev);
            else MDC.remove(TraceIdUtil.SCAN_ID);
            MDC.remove(JOB_NAME);
        }
    }
}
