package com.ratelimiter.core.web;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ratelimiter.core.dtos.RateLimitDecision;
import com.ratelimiter.core.service.RateLimitManager;
import com.ratelimiter.core.utils.RateLimitExceededException;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class RateLimitAspect {

    private final RateLimitManager manager;

    public RateLimitAspect(RateLimitManager manager) {
        this.manager = manager;
    }

    @Around("@annotation(RateLimited)")
    public Object applyLimit(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request =
                ((ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes())
                        .getRequest();

        RateLimitDecision result = manager.evaluate(request);

        if (!result.allowed()) {
            throw new RateLimitExceededException(result);
        }

        return joinPoint.proceed();
    }
}