package com.alvinyang.energycapstone.common.infrastructure

import com.alvinyang.energycapstone.common.domain.DuplicateEntityException
import com.alvinyang.energycapstone.common.domain.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    // Idiom: Static final variable (companion object) or instance variable.
    // "LoggerFactory.getLogger" finds the implementation (Logback) automatically.
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // Handle "Not Found" -> 404
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found")
        problem.type = URI.create("https://api.floenergy.sg/errors/not-found")
        problem.title = "Resource Not Found"
        return problem
    }

    // Handle "Duplicate" -> 409 Conflict
    @ExceptionHandler(DuplicateEntityException::class)
    fun handleDuplicate(ex: DuplicateEntityException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message ?: "Duplicate resource")
        problem.type = URI.create("https://api.floenergy.sg/errors/duplicate")
        problem.title = "Duplicate Resource"
        return problem
    }

    // Handle Validation Errors (@Valid failure) -> 400 Bad Request
    // Spring throws MethodArgumentNotValidException when @Valid fails on a DTO
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed")
        problem.type = URI.create("https://api.floenergy.sg/errors/validation")
        problem.title = "Validation Error"

        // Collect specific field errors
        val errors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        problem.setProperty("fieldErrors", errors) // Add custom property to JSON

        return problem
    }

    // Fallback for unexpected bugs -> 500
    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ProblemDetail {
        // Log the error with the Stack Trace
        // .error(message, exception) prints the stack trace properly
        logger.error("Unexpected error occurred", ex)

        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
        problem.type = URI.create("https://api.floenergy.sg/errors/internal-server-error")
        return problem
    }
}