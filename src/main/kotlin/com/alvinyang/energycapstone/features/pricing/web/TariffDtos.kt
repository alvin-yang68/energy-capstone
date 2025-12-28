package com.alvinyang.energycapstone.features.pricing.web

import com.alvinyang.energycapstone.common.domain.BillingPeriod
import com.alvinyang.energycapstone.common.domain.Country
import com.alvinyang.energycapstone.common.domain.RateType
import com.alvinyang.energycapstone.features.pricing.domain.RateConfiguration
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.time.Instant
import java.util.*

data class CreateTariffPlanRequest(
    @field:NotBlank
    val code: String,

    @field:NotBlank
    val name: String,

    val country: Country,

    val billingPeriod: BillingPeriod,

    @field:NotEmpty
    @field:Valid    // Tells Spring to validate the nested objects too
    val rates: List<CreateTariffRateRequest>
)

data class CreateTariffRateRequest(
    val rateType: RateType,

    @field:NotBlank
    val description: String,

    val configuration: RateConfiguration,   // Jackson Polymorphism works here
)

data class AssignTariffRequest(
    val siteId: UUID,
    val tariffPlanId: UUID,
    val effectiveFrom: Instant
)

data class TariffPlanResponse(
    val id: UUID,
    val name: String,
)

data class AssignmentResponse(
    val id: UUID,
    val siteId: UUID,
    val planId: UUID,
)
