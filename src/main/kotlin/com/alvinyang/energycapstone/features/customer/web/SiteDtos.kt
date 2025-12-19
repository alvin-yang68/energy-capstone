package com.alvinyang.energycapstone.features.customer.web

import com.alvinyang.energycapstone.common.domain.Country
import jakarta.validation.constraints.NotBlank
import java.util.*

data class CreateSiteRequest(
    val customerId: UUID,

    @field:NotBlank(message = "Identifier (NMI/MSSL) is required")
    val identifier: String,

    val country: Country,

    @field:NotBlank(message = "Region is required")
    val region: String,

    val address: String?
)

data class SiteResponse(
    val id: UUID,
    val customerId: UUID,
    val identifier: String,
    val region: String,
    val address: String?,
    val active: Boolean,
    val createdAt: String
)
