package com.alvinyang.energycapstone.features.pricing.domain

import java.math.BigDecimal

data class CalculationResult(
    val totalAmount: BigDecimal,
    val lineItems: List<CalculationLineItem>
)

data class CalculationLineItem(
    val description: String,    // e.g., "Usage Charge (Flat)", "Peak Usage"
    val quantity: BigDecimal,   // kWh
    val rate: BigDecimal?,  // Unit Price (nullable because TOU is blended)
    val amount: BigDecimal
)
