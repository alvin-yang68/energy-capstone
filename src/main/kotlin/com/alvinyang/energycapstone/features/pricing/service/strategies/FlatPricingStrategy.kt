package com.alvinyang.energycapstone.features.pricing.service.strategies

import com.alvinyang.energycapstone.common.domain.RateType
import com.alvinyang.energycapstone.features.metering.domain.MeterReading
import com.alvinyang.energycapstone.features.pricing.domain.CalculationLineItem
import com.alvinyang.energycapstone.features.pricing.domain.FlatRateConfiguration
import com.alvinyang.energycapstone.features.pricing.domain.RateConfiguration
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.ZoneId

@Component
class FlatPricingStrategy : PricingStrategy {
    override val supportedType = RateType.FLAT

    override fun calculateLineItem(
        readings: List<MeterReading>,
        configuration: RateConfiguration,
        description: String,
        zoneId: ZoneId,
    ): CalculationLineItem {
        val config = configuration as? FlatRateConfiguration
            ?: throw IllegalArgumentException("Invalid config for Flat Strategy")

        val totalKwh = readings.fold(BigDecimal.ZERO) { acc, reading -> acc.add(reading.kwh) }
        val cost = totalKwh.multiply(config.pricePerKwh)

        return CalculationLineItem(
            description,
            quantity = totalKwh,
            rate = config.pricePerKwh,
            amount = cost
        )
    }
}