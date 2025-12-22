package com.alvinyang.energycapstone.features.pricing.service

import com.alvinyang.energycapstone.TestcontainersConfiguration
import com.alvinyang.energycapstone.common.domain.BillingPeriod
import com.alvinyang.energycapstone.common.domain.Country
import com.alvinyang.energycapstone.common.domain.RateType
import com.alvinyang.energycapstone.features.customer.domain.Customer
import com.alvinyang.energycapstone.features.customer.domain.Site
import com.alvinyang.energycapstone.features.customer.persistence.CustomerRepository
import com.alvinyang.energycapstone.features.customer.persistence.SiteRepository
import com.alvinyang.energycapstone.features.metering.domain.MeterReading
import com.alvinyang.energycapstone.features.metering.domain.MeterReadingKey
import com.alvinyang.energycapstone.features.metering.persistence.MeterReadingRepository
import com.alvinyang.energycapstone.features.pricing.domain.FlatRateConfiguration
import com.alvinyang.energycapstone.features.pricing.domain.SiteTariffAssignment
import com.alvinyang.energycapstone.features.pricing.domain.TariffPlan
import com.alvinyang.energycapstone.features.pricing.domain.TariffRate
import com.alvinyang.energycapstone.features.pricing.persistence.SiteTariffAssignmentRepository
import com.alvinyang.energycapstone.features.pricing.persistence.TariffPlanRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class PricingServiceIntegrationTest {
    @Autowired lateinit var pricingService: PricingService

    // We need ALL repositories to set up the scenario
    @Autowired lateinit var customerRepo: CustomerRepository
    @Autowired lateinit var siteRepo: SiteRepository
    @Autowired lateinit var tariffRepo: TariffPlanRepository
    @Autowired lateinit var assignmentRepo: SiteTariffAssignmentRepository
    @Autowired lateinit var readingRepo: MeterReadingRepository

    @BeforeEach
    fun cleanup() {
        readingRepo.deleteAll()
        assignmentRepo.deleteAll()
        tariffRepo.deleteAll()
        siteRepo.deleteAll()
        customerRepo.deleteAll()
    }

    @Test
    fun `should calculate prorated invoice when switching plans mid-month`() {
        // --- GIVEN ---

        // 1. Timezone Context (Singapore)
        val sgZone = ZoneId.of("Asia/Singapore")
        val customer = customerRepo.save(Customer(name = "Flo", email = "test@flo.sg", country = Country.SG))
        val site = siteRepo.save(
            Site(
                customer = customer,
                identifier = "NMI-1",
                country = Country.SG,
                region = "North",
                timezone = sgZone,
                address = "123 St"
            )
        )

        // 2. Plan A ($0.20/kWh)
        val planA = createPlan("Plan A", BigDecimal("0.20"))
        // 3. Plan B ($0.30/kWh)
        val planB = createPlan("Plan B", BigDecimal("0.30"))

        // 4. Assignments (Switch happens on Jan 15th)
        val jan01 = Instant.parse("2026-01-01T00:00:00Z")
        val jan15 = Instant.parse("2026-01-15T00:00:00Z")
        val feb01 = Instant.parse("2026-02-01T00:00:00Z")

        assignmentRepo.save(
            SiteTariffAssignment(
                site = site,
                tariffPlan = planA,
                effectiveFrom = jan01,
                effectiveTo = jan15 // Ends halfway
            )
        )
        assignmentRepo.save(SiteTariffAssignment(
            site = site,
            tariffPlan = planB,
            effectiveFrom = jan15,
            effectiveTo = null // Ongoing
        ))

        // 5. Meter Readings
        // Reading on Jan 05 (Should be Plan A)
        val r1 = MeterReading(
            id = MeterReadingKey(site.id, Instant.parse("2026-01-05T12:00:00Z")),
            kwh = BigDecimal("100.00"),
            site = site
        )
        // Reading on Jan 20 (Should be Plan B)
        val r2 = MeterReading(
            id = MeterReadingKey(site.id, Instant.parse("2026-01-20T12:00:00Z")),
            kwh = BigDecimal("100.00"),
            site = site
        )
        readingRepo.saveAll(listOf(r1, r2))

        // --- WHEN ---
        val result = pricingService.calculateDraftInvoice(site.id, jan01, feb01)

        // --- THEN ---

        // 1. Total Amount
        // Plan A: 100 * 0.20 = 20.00
        // Plan B: 100 * 0.30 = 30.00
        // Total: 50.00
        assertThat(result.totalAmount).isEqualByComparingTo("50.00")

        // 2. Line Items
        assertThat(result.lineItems).hasSize(2)

        // Check Item 1 (Plan A)
        val itemA = result.lineItems.find { it.description.contains("Plan A") }!!
        assertThat(itemA.amount).isEqualByComparingTo("20.00")
        // Verify Description contains date range (because we had >1 assignment)
        assertThat(itemA.description).contains("Jan 01 - Jan 15")

        // Check Item 2 (Plan B)
        val itemB = result.lineItems.find { it.description.contains("Plan B") }!!
        assertThat(itemB.amount).isEqualByComparingTo("30.00")
        assertThat(itemB.description).contains("Jan 15 - Feb 01")
    }

    // Helper to reduce boilerplate
    private fun createPlan(name: String, pricePerKwh: BigDecimal): TariffPlan {
        val plan = TariffPlan(
            code = name.uppercase().replace(" ", "-"),
            name = name,
            country = Country.SG,
            billingPeriod = BillingPeriod.MONTHLY,
            validFrom = Instant.now()
        )
        val rate = TariffRate(
            tariffPlan = plan,
            rateType = RateType.FLAT,
            description = "Usage Charge",
            configuration = FlatRateConfiguration(pricePerKwh)
        )
        plan.rates.add(rate)
        return tariffRepo.save(plan)
    }
}