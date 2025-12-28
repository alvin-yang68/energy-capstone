package com.alvinyang.energycapstone.features.metering.web

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class IngestReadingRequest(
    val siteId: UUID,
    val readAt: Instant,
    val kwh: BigDecimal,
)
