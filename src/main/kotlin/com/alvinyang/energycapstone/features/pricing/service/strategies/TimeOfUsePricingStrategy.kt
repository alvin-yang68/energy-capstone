package com.alvinyang.energycapstone.features.pricing.service.strategies

import com.alvinyang.energycapstone.common.domain.RateType
import com.alvinyang.energycapstone.features.metering.domain.MeterReading
import com.alvinyang.energycapstone.features.pricing.domain.RateConfiguration
import com.alvinyang.energycapstone.features.pricing.domain.TimeOfUseConfiguration
import java.math.BigDecimal
import java.time.ZoneId

class TimeOfUsePricingStrategy : PricingStrategy {
    override val supportedType = RateType.TIME_OF_USE

    override fun calculateCost(
        readings: List<MeterReading>,
        configuration: RateConfiguration,
        zoneId: ZoneId
    ): BigDecimal {
        val config = configuration as? TimeOfUseConfiguration
            ?: throw IllegalArgumentException("Invalid config for TOU Strategy")

        var totalCost = BigDecimal.ZERO
        for (reading in readings) {
            // Convert UTC Instant -> Wall Clock Time (LocalTime) at the Site
            val readAt = reading.id.readAt.atZone(zoneId).toLocalTime()

            // Check if Peak Range wraps around midnight (e.g. 22:00 to 06:00)
            val wrapsAroundMidnight = config.peakEnd.isBefore(config.peakStart)

            val isPeak = if (wrapsAroundMidnight) {
                // Midnight Case: 22:00 to 06:00
                // Logic: Time is AFTER Start OR Time is NOT AFTER End
                readAt.isAfter(config.peakStart) || !readAt.isAfter(config.peakEnd)
            } else {
                // Normal Case: 09:00 to 21:00
                // Logic: Time is AFTER Start AND Time is NOT AFTER End
                readAt.isAfter(config.peakStart) && !readAt.isAfter(config.peakEnd)
            }

            val rate = if (isPeak) config.peakPrice else config.offPeakPrice
            totalCost = totalCost.add(reading.kwh.multiply(rate))
        }

        return totalCost
    }
}