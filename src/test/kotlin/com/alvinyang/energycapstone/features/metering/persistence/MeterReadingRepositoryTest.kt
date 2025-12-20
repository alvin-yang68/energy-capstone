package com.alvinyang.energycapstone.features.metering.persistence

import com.alvinyang.energycapstone.TestcontainersConfiguration
import com.alvinyang.energycapstone.common.domain.Country
import com.alvinyang.energycapstone.features.customer.domain.Customer
import com.alvinyang.energycapstone.features.customer.domain.Site
import com.alvinyang.energycapstone.features.customer.persistence.CustomerRepository
import com.alvinyang.energycapstone.features.customer.persistence.SiteRepository
import com.alvinyang.energycapstone.features.metering.domain.MeterReading
import com.alvinyang.energycapstone.features.metering.domain.MeterReadingKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.Instant

@DataJpaTest
@Import(TestcontainersConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MeterReadingRepositoryTest {
    @Autowired
    lateinit var meterReadingRepository: MeterReadingRepository

    @Autowired
    lateinit var siteRepository: SiteRepository

    @Autowired
    lateinit var customerRepository: CustomerRepository

    @Test
    fun `should save and retrieve readings ordered by time`() {
        // 1. Setup Parent Entities
        val customer = customerRepository.save(Customer(name = "Test", email = "t@t.com", country = Country.SG))
        val site = siteRepository.save(Site(customer = customer, identifier = "M1", country = Country.SG, region = "N"))

        // 2. Save Readings (Out of order insertion)
        val now = Instant.now()
        val r1 = MeterReading(
            id = MeterReadingKey(siteId = site.id, readAt = now.minusSeconds(3600)),
            kwh = BigDecimal("1.5"),
            site
        )
        val r2 = MeterReading(id = MeterReadingKey(siteId = site.id, readAt = now), kwh = BigDecimal("2.0"), site)

        meterReadingRepository.saveAll(listOf(r2, r1))  // Save r2 (later) then r1 (earlier)

        // 3. Retrieve
        val readings = meterReadingRepository.findBySiteIdAndDateRange(
            siteId = site.id,
            start = now.minusSeconds(7200),
            end = now.plusSeconds(1)
        )

        // 4. Assert
        assertThat(readings).hasSize(2)
        assertThat(readings[0].kwh).isEqualByComparingTo(BigDecimal("1.5"))    // Should be r1 (earlier)
        assertThat(readings[1].kwh).isEqualByComparingTo(BigDecimal("2.0"))    // Should be r2 (later)
    }
}