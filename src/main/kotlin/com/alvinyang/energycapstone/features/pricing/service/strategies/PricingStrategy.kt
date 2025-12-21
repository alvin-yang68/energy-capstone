package com.alvinyang.energycapstone.features.pricing.service.strategies

import com.alvinyang.energycapstone.common.domain.RateType
import com.alvinyang.energycapstone.features.metering.domain.MeterReading
import com.alvinyang.energycapstone.features.pricing.domain.RateConfiguration
import java.math.BigDecimal
import java.time.ZoneId

interface PricingStrategy {
    // Each strategy knows which RateType it handles (e.g., FLAT, TOU)
    val supportedType: RateType

    fun calculateCost(
        readings: List<MeterReading>,
        configuration: RateConfiguration,
        zoneId: ZoneId  // Crucial for converting Instant -> LocalTime for TOU
    ): BigDecimal
}