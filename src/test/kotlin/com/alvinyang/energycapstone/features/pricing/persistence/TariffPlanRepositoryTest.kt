package com.alvinyang.energycapstone.features.pricing.persistence

import com.alvinyang.energycapstone.TestcontainersConfiguration
import com.alvinyang.energycapstone.common.domain.BillingPeriod
import com.alvinyang.energycapstone.common.domain.Country
import com.alvinyang.energycapstone.common.domain.RateType
import com.alvinyang.energycapstone.features.pricing.domain.TariffPlan
import com.alvinyang.energycapstone.features.pricing.domain.TariffRate
import com.alvinyang.energycapstone.features.pricing.domain.TimeOfUseConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalTime

@DataJpaTest    // 1. Slices the context (only loads DB stuff, not Controllers)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)    // Don't replace the real DB with H2
@Import(TestcontainersConfiguration::class)
class TariffPlanRepositoryTest {
    @Autowired
    lateinit var tariffPlanRepository: TariffPlanRepository

    @Autowired
    lateinit var entityManager: TestEntityManager   // Helper to interact with L1 cache

    @Test
    fun `should persist tariff plan with JSONB rates`() {
        // Given
        val newTariffPlan = TariffPlan(
            code = "SG-FLAT-2026",
            name = "Singapore Flat Rate",
            country = Country.SG,
            billingPeriod = BillingPeriod.MONTHLY,
            validFrom = Instant.now()
        )
        val newRateConfig = TimeOfUseConfiguration(
            peakPrice = BigDecimal("0.35"),
            offPeakPrice = BigDecimal("0.15"),
            peakStart = LocalTime.of(18, 30),
            peakEnd = LocalTime.of(22, 0)
        )
        val newTariffRate = TariffRate(
            tariffPlan = newTariffPlan,
            rateType = RateType.TIME_OF_USE,
            configuration = newRateConfig
        )
        newTariffPlan.rates.add(newTariffRate)

        // When
        val savedTariffPlan = tariffPlanRepository.save(newTariffPlan)

        // Critical Step: FLUSH and CLEAR the cache.
        // If we don't do this, findById returns the object strictly from memory.
        // We want to force Hibernate to READ from the actual DB (triggering the JSON converter).
        entityManager.flush()
        entityManager.clear()

        // Then
        val loadedTariffPlan = tariffPlanRepository.findById(savedTariffPlan.id).orElseThrow()
        assertThat(loadedTariffPlan.rates).hasSize(1)

        val loadedTariffRate = loadedTariffPlan.rates[0]
        assertThat(loadedTariffRate.rateType).isEqualTo(RateType.TIME_OF_USE)
        assertThat(loadedTariffRate.configuration).isInstanceOf(TimeOfUseConfiguration::class.java)

        val loadedRateConfig = loadedTariffRate.configuration as TimeOfUseConfiguration
        assertThat(loadedRateConfig.peakPrice).isEqualTo(BigDecimal("0.35"))
        assertThat(loadedRateConfig.peakStart).isEqualTo(LocalTime.of(18, 30))
    }
}