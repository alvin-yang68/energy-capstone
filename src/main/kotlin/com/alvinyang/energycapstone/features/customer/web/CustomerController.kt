package com.alvinyang.energycapstone.features.customer.web

import com.alvinyang.energycapstone.features.customer.service.CustomerService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/customers")
class CustomerController(
    private val customerService: CustomerService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Returns 201 instead of 200
    fun createCustomer(@Valid @RequestBody request: CreateCustomerRequest): CustomerResponse {
        // @Valid triggers the DTO annotations (@NotBlank).
        // If invalid, Spring throws MethodArgumentNotValidException (handled globally).
        return customerService.createCustomer(request)
    }
}