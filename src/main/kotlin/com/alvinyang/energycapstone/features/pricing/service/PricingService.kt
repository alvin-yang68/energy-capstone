package com.alvinyang.energycapstone.features.pricing.service

import com.alvinyang.energycapstone.common.domain.ResourceNotFoundException
import com.alvinyang.energycapstone.features.customer.persistence.SiteRepository
import com.alvinyang.energycapstone.features.metering.persistence.MeterReadingRepository
import com.alvinyang.energycapstone.features.pricing.domain.CalculationLineItem
import com.alvinyang.energycapstone.features.pricing.domain.CalculationResult
import com.alvinyang.energycapstone.features.pricing.persistence.SiteTariffAssignmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class PricingService(
    private val siteRepository: SiteRepository,
    private val assignmentRepository: SiteTariffAssignmentRepository,
    private val meterReadingRepository: MeterReadingRepository,
    private val strategyFactory: PricingStrategyFactory
) {

    // Purely calculation - does not save an invoice.
    // readOnly = true is a performance hint for Hibernate (skips dirty checking).
    @Transactional(readOnly = true)
    fun calculateDraftInvoice(siteId: UUID, billingPeriodStart: Instant, billingPeriodEnd: Instant): CalculationResult {
        // 1. Get Context (Timezone)
        val context =
            siteRepository.findPricingContextById(siteId) ?: throw ResourceNotFoundException("Site $siteId not found")
        val zoneId = context.timezone

        // 2. Find all relevant Tariff Plans (Proration Logic)
        val assignments = assignmentRepository.findOverlappingAssignment(siteId, billingPeriodStart, billingPeriodEnd)
        if (assignments.isEmpty()) {
            // Edge case: No plan
            return CalculationResult(totalAmount = BigDecimal.ZERO, lineItems = emptyList())
        }

        // 3. Fetch readings for the entire billing period
        val allReadings = meterReadingRepository.findBySiteIdAndDateRange(siteId, billingPeriodStart, billingPeriodEnd)

        var totalAmount = BigDecimal.ZERO
        val allLineItems = mutableListOf<CalculationLineItem>()

        // Date Formatter (e.g., "Jan 01")
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd").withZone(zoneId)

        // 4. Process each time slice
        for (assignment in assignments) {
            // Calculate the specific window for this plan within the billing period
            // Max(billStart, planStart)
            val effectiveStart = maxOf(billingPeriodStart, assignment.effectiveFrom)

            // Min(billEnd, planEnd) -> handle null effectiveTo as "Infinity"
            val effectiveEnd = assignment.effectiveTo?.let { minOf(billingPeriodEnd, it) } ?: billingPeriodEnd

            // Skip if invalid range (shouldn't happen due to query, but safety check)
            if (!effectiveStart.isBefore(effectiveEnd)) continue

            // 5. Filter readings for this time slice
            val sliceReadings = allReadings.filter { reading ->
                val readAt = reading.id.readAt
                readAt.isAfter(effectiveStart) && !readAt.isAfter(effectiveEnd)
            }
            if (sliceReadings.isEmpty()) continue

            // 6. Calculate Cost for EACH Rate in the Plan (Multi-Rate Logic)
            val plan = assignment.tariffPlan
            for (rate in plan.rates) {
                val strategy = strategyFactory.getStrategy(rate.rateType)

                val description = if (assignments.size > 1) {
                    // If there is more than 1 assignment involved, it's a proration scenario.
                    // Proration: "Plan A - Usage Charge (Jan 01 - Jan 15)"
                    val dateRange = "${dateFormatter.format(effectiveStart)} - ${dateFormatter.format(effectiveEnd)}"
                    "${plan.name} - ${rate.description} ($dateRange)"
                } else {
                    rate.description
                }

                val lineItem = strategy.calculateLineItem(
                    readings = sliceReadings,
                    configuration = rate.configuration,
                    description,
                    zoneId
                )

                totalAmount = totalAmount.add(lineItem.amount)
                allLineItems.add(lineItem)
            }
        }

        return CalculationResult(totalAmount, allLineItems)
    }
}