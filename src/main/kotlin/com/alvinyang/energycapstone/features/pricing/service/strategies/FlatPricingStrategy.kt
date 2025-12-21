package com.alvinyang.energycapstone.features.pricing.service.strategies

import com.alvinyang.energycapstone.common.domain.RateType
import com.alvinyang.energycapstone.features.metering.domain.MeterReading
import com.alvinyang.energycapstone.features.pricing.domain.FlatRateConfiguration
import com.alvinyang.energycapstone.features.pricing.domain.RateConfiguration
import java.math.BigDecimal
import java.time.ZoneId

class FlatPricingStrategy : PricingStrategy {
    override val supportedType = RateType.FLAT

    override fun calculateCost(
        readings: List<MeterReading>,
        configuration: RateConfiguration,
        zoneId: ZoneId
    ): BigDecimal {
        val config = configuration as? FlatRateConfiguration
            ?: throw IllegalArgumentException("Invalid config for Flat Strategy")

        val totalKwh = readings.fold(BigDecimal.ZERO) { acc, reading -> acc.add(reading.kwh) }

        return totalKwh.multiply(config.pricePerKwh)
    }
}