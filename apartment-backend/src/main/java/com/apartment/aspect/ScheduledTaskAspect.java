package com.apartment.aspect;

import com.apartment.entity.ScheduledTaskLog;
import com.apartment.repository.ScheduledTaskLogRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ScheduledTaskAspect {

    @Autowired
    private ScheduledTaskLogRepository scheduledTaskLogRepository;

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object logScheduledTask(ProceedingJoinPoint joinPoint) throws Throwable {
        String taskName = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();
        ScheduledTaskLog log = new ScheduledTaskLog();
        log.setTaskName(taskName);

        try {
            Object result = joinPoint.proceed();
            log.setStatus("success");
            return result;
        } catch (Throwable t) {
            log.setStatus("fail");
            String reason = t.getMessage();
            if (reason != null && reason.length() > 500) {
                reason = reason.substring(0, 500);
            }
            log.setFailReason(reason);
            throw t;
        } finally {
            log.setDuration(System.currentTimeMillis() - start);
            try {
                scheduledTaskLogRepository.save(log);
            } catch (Exception e) {
                // Avoid breaking scheduled task if logging fails
            }
        }
    }
}
