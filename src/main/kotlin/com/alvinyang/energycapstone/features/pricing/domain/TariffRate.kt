package com.alvinyang.energycapstone.features.pricing.domain

import com.alvinyang.energycapstone.common.domain.RateType
import jakarta.persistence.*
import org.hibernate.annotations.ColumnTransformer
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.*

@Entity
@Table(name = "tariff_rate")
class TariffRate(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_plan_id", nullable = false)
    val tariffPlan: TariffPlan,

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false)
    val rateType: RateType,

    // 1. Convert Object -> String (using our explicit Converter)
    // 2. Cast String -> JSONB (using explicit SQL)
    @Convert(converter = RateConfigurationConverter::class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "configuration", columnDefinition = "jsonb", nullable = false)
    val configuration: RateConfiguration
) {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
}