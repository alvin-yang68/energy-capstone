package com.alvinyang.energycapstone.features.billing.service

import com.alvinyang.energycapstone.TestcontainersConfiguration
import com.alvinyang.energycapstone.common.domain.BillingPeriod
import com.alvinyang.energycapstone.common.domain.Country
import com.alvinyang.energycapstone.common.domain.Currency
import com.alvinyang.energycapstone.common.domain.RateType
import com.alvinyang.energycapstone.features.billing.domain.InvoiceStatus
import com.alvinyang.energycapstone.features.billing.persistence.InvoiceRepository
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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class InvoiceServiceIntegrationTest {
    @Autowired
    lateinit var invoiceService: InvoiceService

    @Autowired
    lateinit var invoiceRepo: InvoiceRepository

    // Repos for setup
    @Autowired
    lateinit var customerRepo: CustomerRepository

    @Autowired
    lateinit var siteRepo: SiteRepository

    @Autowired
    lateinit var tariffRepo: TariffPlanRepository

    @Autowired
    lateinit var assignmentRepo: SiteTariffAssignmentRepository

    @Autowired
    lateinit var readingRepo: MeterReadingRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanup() {
        readingRepo.deleteAll()
        assignmentRepo.deleteAll()
        tariffRepo.deleteAll()
        invoiceRepo.deleteAll() // Delete invoices before customers
        siteRepo.deleteAll()
        customerRepo.deleteAll()
    }

    @Test
    fun `should generate correct invoice and prevent overlaps`() {
        // --- GIVEN ---
        val customer = customerRepo.save(Customer(name = "Flo", email = "test@flo.sg", country = Country.SG))

        // Setup Site & Tariff
        val site = createSite(customer)
        val plan = createPlan("Standard Plan", BigDecimal("0.25"))
        assignPlan(site, plan)

        // Insert usage: 100 kWh @ $0.25 = $25.00
        val readingTime = Instant.parse("2026-01-15T12:00:00Z")
        readingRepo.save(
            MeterReading(
                id = MeterReadingKey(site.id, readingTime),
                kwh = BigDecimal("100.00"),
                site = site
            )
        )

        val jan01 = Instant.parse("2026-01-01T00:00:00Z")
        val feb01 = Instant.parse("2026-02-01T00:00:00Z")

        // --- WHEN (First Generation) ---
        val invoice = invoiceService.generateInvoiceForCustomer(
            customerId = customer.id,
            billingPeriodStart = jan01,
            billingPeriodEnd = feb01
        )
        println("Invoice 1: ${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(invoice)}")

        // --- THEN (Verify Persistence) ---
        assertThat(invoice.totalAmount).isEqualByComparingTo("25.00")
        assertThat(invoice.status).isEqualTo(InvoiceStatus.DRAFT)
        assertThat(invoice.currency).isEqualTo(Currency.SGD)
        assertThat(invoice.lines).hasSize(2) // 1 Header + 1 Usage Line

        // Verify Header
        assertThat(invoice.lines[0].description).contains(site.identifier)

        // Verify Usage Line (Snapshot of cost)
        val usageLine = invoice.lines[1]
        assertThat(usageLine.amount).isEqualByComparingTo("25.00")
        assertThat(usageLine.quantity).isEqualByComparingTo("100.00")
        assertThat(usageLine.unitPrice).isEqualByComparingTo("0.25")

        // --- WHEN (Try to Overlap) ---
        // Try generating for Jan 15 - Feb 15 (Overlaps Jan 15-31)
        val midJan = Instant.parse("2026-01-15T00:00:00Z")
        val midFeb = Instant.parse("2026-02-15T00:00:00Z")

        assertThatThrownBy {
            invoiceService.generateInvoiceForCustomer(
                customerId = customer.id,
                billingPeriodStart = midJan,
                billingPeriodEnd = midFeb
            )
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("overlapping")

        // --- WHEN (Void and Re-bill) ---
        // 1. Void the original invoice
        invoice.status = InvoiceStatus.VOID
        invoiceRepo.save(invoice)

        // 2. Retry generation (Should work now because status is VOID)
        val newInvoice = invoiceService.generateInvoiceForCustomer(
            customerId = customer.id,
            billingPeriodStart = jan01,
            billingPeriodEnd = feb01
        )
        println("Invoice 2: ${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(invoice)}")
        assertThat(newInvoice.id).isNotEqualTo(invoice.id)
        assertThat(newInvoice.totalAmount).isEqualByComparingTo("25.00")
    }

    // --- Helpers ---

    private fun createSite(customer: Customer): Site {
        return siteRepo.save(
            Site(
                customer = customer,
                identifier = "NMI-1",
                country = Country.SG,
                region = "North",
                timezone = ZoneId.of("Asia/Singapore"),
                address = "123 St"
            )
        )
    }

    private fun createPlan(name: String, price: BigDecimal): TariffPlan {
        val plan = TariffPlan(
            code = "PLAN", name = name, country = Country.SG,
            billingPeriod = BillingPeriod.MONTHLY, validFrom = Instant.now()
        )
        plan.rates.add(
            TariffRate(
                tariffPlan = plan, rateType = RateType.FLAT, description = "Usage",
                configuration = FlatRateConfiguration(price)
            )
        )
        return tariffRepo.save(plan)
    }

    private fun assignPlan(site: Site, plan: TariffPlan) {
        assignmentRepo.save(
            SiteTariffAssignment(
                site = site, tariffPlan = plan,
                effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"), effectiveTo = null
            )
        )
    }
}