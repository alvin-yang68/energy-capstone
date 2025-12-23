package com.alvinyang.energycapstone.common.infrastructure

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class LoggingAspect {
    private val logger = LoggerFactory.getLogger(LoggingAspect::class.java)

    // Define "Where" to apply logic (Pointcut)
    // "Around any function annotated with @LogExecutionTime"
    @Around("@annotation(com.alvinyang.energycapstone.common.infrastructure.LogExecutionTime)")
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val start = System.currentTimeMillis()

        // Execute the actual method
        val result = joinPoint.proceed()

        val executionTime = System.currentTimeMillis() - start

        // Log after execution
        logger.info("${joinPoint.signature.name} executed in ${executionTime}ms")

        return result
    }
}