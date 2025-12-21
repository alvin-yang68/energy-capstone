package com.alvinyang.energycapstone.features.pricing.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigDecimal
import java.time.LocalTime

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, // Use a string name to identify the type
    include = JsonTypeInfo.As.PROPERTY, // As a property inside the JSON object
    property = "type",   // The JSON field name (e.g., "type": "FLAT")
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FlatRateConfiguration::class, name = "FLAT")
)
sealed interface RateConfiguration

data class FlatRateConfiguration(
    val pricePerKwh: BigDecimal,
) : RateConfiguration

data class TimeOfUseConfiguration(
    val peakPrice: BigDecimal,
    val offPeakPrice: BigDecimal,
    val peakStart: LocalTime,
    val peakEnd: LocalTime
) : RateConfiguration