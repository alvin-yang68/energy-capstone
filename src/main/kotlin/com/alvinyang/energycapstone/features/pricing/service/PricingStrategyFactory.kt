package com.alvinyang.energycapstone.features.pricing.service

import com.alvinyang.energycapstone.common.domain.RateType
import com.alvinyang.energycapstone.features.pricing.service.strategies.PricingStrategy
import org.springframework.stereotype.Component

@Component
class PricingStrategyFactory(
    strategies: List<PricingStrategy>
) {
    // Convert the List to a fast Map for lookups
    private val strategyMap = strategies.associateBy { it.supportedType }

    fun getStrategy(rateType: RateType): PricingStrategy {
        return strategyMap[rateType]
            ?: throw IllegalArgumentException("No strategy implementation found for rate type: $rateType")
    }
}