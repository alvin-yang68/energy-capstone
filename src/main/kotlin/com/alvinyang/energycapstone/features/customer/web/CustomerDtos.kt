package com.alvinyang.energycapstone.features.customer.web

import com.alvinyang.energycapstone.common.domain.Country
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.*

// Input DTO
data class CreateCustomerRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:Email(message = "Invalid email format")
    @field:NotBlank(message = "Email is required")
    val email: String,

    val country: Country,   // Enums are automatically validated for valid values by Jackson
)

// Output DTO
data class CustomerResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val country: Country,
    val createdAt: String,  // Return ISO string to frontend
)
