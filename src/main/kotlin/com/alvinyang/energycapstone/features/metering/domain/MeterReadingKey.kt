package com.alvinyang.energycapstone.features.metering.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.time.Instant
import java.util.*

@Embeddable
data class MeterReadingKey(
    @Column(name = "site_id", nullable = false)
    val siteId: UUID,

    @Column(name = "read_at", nullable = false)
    val readAt: Instant
) : Serializable
