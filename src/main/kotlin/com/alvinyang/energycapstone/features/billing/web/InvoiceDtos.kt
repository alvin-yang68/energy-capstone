package com.alvinyang.energycapstone.features.billing.web

import java.time.Instant
import java.util.*

data class PreviewInvoiceRequest(
    val siteId: UUID,
    val start: Instant,
    val end: Instant,
)

