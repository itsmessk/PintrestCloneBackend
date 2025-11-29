package com.infy.pinterest.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @AfterThrowing(pointcut = "execution(* com.infy.pinterest.service.*.*(..))", throwing = "exception")
    public void logServiceException(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        log.error("Exception in {}.{} with arguments: {} - Exception: {}",
                className,
                methodName,
                Arrays.toString(args),
                exception.getMessage(),
                exception);
    }
}
