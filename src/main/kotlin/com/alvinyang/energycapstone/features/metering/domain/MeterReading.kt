package com.alvinyang.energycapstone.features.metering.domain

import com.alvinyang.energycapstone.features.customer.domain.Site
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "meter_reading")
class MeterReading(
    @EmbeddedId
    val id: MeterReadingKey,

    @Column(nullable = false, precision = 12, scale = 4)
    val kwh: BigDecimal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    @MapsId("siteId")   // Maps to the `siteId` field in the composite key
    val site: Site
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeterReading) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "MeterReading(id=$id)"
}