package com.alvinyang.energycapstone.features.billing.service

import com.alvinyang.energycapstone.common.domain.ResourceNotFoundException
import com.alvinyang.energycapstone.features.billing.domain.Invoice
import com.alvinyang.energycapstone.features.billing.domain.InvoiceLine
import com.alvinyang.energycapstone.features.billing.domain.InvoiceStatus
import com.alvinyang.energycapstone.features.billing.persistence.InvoiceRepository
import com.alvinyang.energycapstone.features.customer.persistence.CustomerRepository
import com.alvinyang.energycapstone.features.customer.persistence.SiteRepository
import com.alvinyang.energycapstone.features.pricing.service.PricingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val customerRepository: CustomerRepository,
    private val siteRepository: SiteRepository,
    private val pricingService: PricingService
) {
    @Transactional
    fun generateInvoiceForCustomer(customerId: UUID, billingPeriodStart: Instant, billingPeriodEnd: Instant): Invoice {
        // 1. Validation
        if (invoiceRepository.existsActiveOverlappingInvoice(customerId, billingPeriodStart, billingPeriodEnd)) {
            // Idempotency: Don't bill twice.
            // Real apps might return the existing invoice, here we throw.
            throw IllegalStateException(
                "An active invoice already exists overlapping the period $billingPeriodStart to $billingPeriodEnd for customer $customerId"
            )
        }

        val customer = customerRepository.findById(customerId)
            .orElseThrow { ResourceNotFoundException("Customer $customerId not found") }

        // 2. Find Sites
        val sites = siteRepository.findAllByCustomerIdAndActiveTrue(customerId)
        if (sites.isEmpty()) {
            // Edge case: Customer has no active sites. Skip or generate $0 invoice?
            throw IllegalStateException("No active sites for customer")
        }

        // 3. Create Shell Invoice
        val invoice = Invoice(
            customer = customer,
            billingPeriodStart = billingPeriodStart,
            billingPeriodEnd = billingPeriodEnd,
            // TODO: save the payment due date in the customer table instead of hardcoding to 14 days
            dueDate = billingPeriodEnd.plus(14, ChronoUnit.DAYS),
            totalAmount = BigDecimal.ZERO,
            currency = customer.country.currency,
            status = InvoiceStatus.DRAFT
        )

        var lineIndex = 0

        // 4. Calculate & Aggregate
        for (site in sites) {
            val result = pricingService.calculateDraftInvoice(siteId = site.id, billingPeriodStart, billingPeriodEnd)

            // Add Header Line for the Site (UX improvement)
            invoice.lines.add(
                InvoiceLine(
                    invoice = invoice,
                    description = "Site: ${site.identifier} (${site.region})",
                    quantity = BigDecimal.ZERO,
                    unitPrice = null,
                    amount = BigDecimal.ZERO,   // Header lines usually $0
                    index = lineIndex++
                )
            )

            // Add the actual calculation lines
            for (item in result.lineItems) {
                invoice.lines.add(
                    InvoiceLine(
                        invoice = invoice,
                        description = item.description,
                        quantity = item.quantity,
                        unitPrice = item.rate,
                        amount = item.amount,
                        index = lineIndex++
                    )
                )
            }
            invoice.totalAmount = invoice.totalAmount.add(result.totalAmount)
        }

        return invoiceRepository.save(invoice)
    }
}