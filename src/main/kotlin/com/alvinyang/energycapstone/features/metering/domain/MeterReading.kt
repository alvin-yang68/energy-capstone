package com.alvinyang.energycapstone.features.metering.domain

import com.alvinyang.energycapstone.features.customer.domain.Site
import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.math.BigDecimal

@Entity
@Table(name = "meter_reading")
class MeterReading(
    @EmbeddedId
    private val _id: MeterReadingKey,    // Rename backing field to avoid conflict with getId()

    @Column(nullable = false, precision = 12, scale = 4)
    val kwh: BigDecimal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    @MapsId("siteId")   // Maps to the `siteId` field in the composite key
    val site: Site
) : Persistable<MeterReadingKey> {
    // Expose the ID via the interface method
    override fun getId(): MeterReadingKey = _id

    // We explicitly tell Spring "This object is NEW".
    // This skips the SELECT and forces an INSERT.
    // Note: If you try to insert a duplicate ID, Postgres will throw an error (which is what we want),
    // rather than Spring updating the existing row.
    override fun isNew(): Boolean = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeterReading) return false
        return _id == other._id
    }

    override fun hashCode(): Int = _id.hashCode()

    override fun toString(): String = "MeterReading(id=$_id)"
}