package com.alvinyang.energycapstone.common.domain

enum class RateType {
    FLAT,           // Simple price per kWh
    TIME_OF_USE,    // Peak vs Off-Peak based on time
    BLOCK           // Tiered (First 100kWh @ $X, Next @ $Y)
}