package com.alvinyang.energycapstone.features.pricing.service.strategies

import com.alvinyang.energycapstone.features.customer.domain.Site
import com.alvinyang.energycapstone.features.metering.domain.MeterReading
import com.alvinyang.energycapstone.features.metering.domain.MeterReadingKey
import com.alvinyang.energycapstone.features.pricing.domain.FlatRateConfiguration
import com.alvinyang.energycapstone.features.pricing.domain.TimeOfUseConfiguration
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

class PricingStrategyTest {
    @Test
    fun `Flat Strategy calculates total correctly`() {
        val strategy = FlatPricingStrategy()
        val configuration = FlatRateConfiguration(pricePerKwh = BigDecimal("0.50"))

        val readings = listOf(
            reading("10.0", "2026-01-01T10:00:00Z"),
            reading("5.0", "2026-01-01T10:30:00Z"),
        )

        // 15 kWh * $0.50 = $7.50
        val lineItem = strategy.calculateLineItem(
            readings,
            configuration,
            description = "Energy Charge",
            zoneId = ZoneId.of("UTC")
        )

        assertThat(lineItem.amount).isEqualByComparingTo("7.50")
        assertThat(lineItem.quantity).isEqualByComparingTo("15.00") // Total Usage
        assertThat(lineItem.rate).isEqualByComparingTo("0.50")      // Unit Price
        assertThat(lineItem.description).isEqualTo("Energy Charge") // Passthrough
    }

    @Test
    fun `TOU Strategy applies peak and off-peak rates correctly`() {
        val strategy = TimeOfUsePricingStrategy()
        val zoneId = ZoneId.of("Asia/Singapore")
        val configuration = TimeOfUseConfiguration(
            peakPrice = BigDecimal("1.00"),
            offPeakPrice = BigDecimal("0.50"),
            // Peak: 12:00 to 14:00
            peakStart = LocalTime.of(12, 0),
            peakEnd = LocalTime.of(14, 0)
        )

        val readings = listOf(
            // 12:00 reading (usage 11:30-12:00) -> SHOULD BE OFF-PEAK
            reading("10.0", "2026-01-01T04:00:00Z"), // 12:00 SG

            // 12:30 reading (usage 12:00-12:30) -> SHOULD BE PEAK
            reading("10.0", "2026-01-01T04:30:00Z"), // 12:30 SG

            // 14:00 reading (usage 13:30-14:00) -> SHOULD BE PEAK
            reading("10.0", "2026-01-01T06:00:00Z"), // 14:00 SG

            // 14:30 reading (usage 14:00-14:30) -> SHOULD BE OFF-PEAK
            reading("10.0", "2026-01-01T06:30:00Z")  // 14:30 SG
        )

        val lineItem = strategy.calculateLineItem(readings, configuration, description = "TOU Charge", zoneId)

        // Math:
        // 12:00 (Off): 10 * 0.50 = 5.0
        // 12:30 (Peak): 10 * 1.00 = 10.0
        // 14:00 (Peak): 10 * 1.00 = 10.0
        // 14:30 (Off): 10 * 0.50 = 5.0
        // Total = 30.00
        assertThat(lineItem.amount).isEqualByComparingTo("30.00")

        // Total Quantity: 40 kWh
        assertThat(lineItem.quantity).isEqualByComparingTo("40.00")

        // Rate should be null because it's a mix of $0.50 and $1.00
        assertThat(lineItem.rate).isNull()

        assertThat(lineItem.description).isEqualTo("TOU Charge")
    }

    // Helper to create a dummy reading
    private fun reading(kwh: String, timeIso: String): MeterReading {
        val instant = Instant.parse(timeIso)
        return MeterReading(
            id = MeterReadingKey(siteId = UUID.randomUUID(), readAt = instant),
            kwh = BigDecimal(kwh),
            site = mockk<Site>()    // We mock Site because we don't need it for calculation, only the ID/Time matters
        )
    }
}