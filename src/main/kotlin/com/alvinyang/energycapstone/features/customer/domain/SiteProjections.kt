package com.alvinyang.energycapstone.features.customer.domain

import java.time.ZoneId
import java.util.*

// Only the fields we need for pricing
data class SitePricingContext(
    val id: UUID,
    val timezone: ZoneId
)
