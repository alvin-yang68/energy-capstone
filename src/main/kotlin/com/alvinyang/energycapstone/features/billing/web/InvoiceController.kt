package com.alvinyang.energycapstone.features.billing.web

import com.alvinyang.energycapstone.features.pricing.domain.CalculationResult
import com.alvinyang.energycapstone.features.pricing.service.PricingService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/invoices")
class InvoiceController(
    private val pricingService: PricingService
) {

    @PostMapping("/preview")
    fun previewInvoice(@Valid @RequestBody request: PreviewInvoiceRequest): CalculationResult {
        // Basic date validation
        if (!request.start.isBefore(request.end)) {
            throw IllegalArgumentException("Start date must be before end date")
        }

        return pricingService.calculateDraftInvoice(
            siteId = request.siteId,
            billingPeriodStart = request.start,
            billingPeriodEnd = request.end
        )
    }
}