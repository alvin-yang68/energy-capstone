package com.alvinyang.energycapstone.features.pricing.domain

import com.alvinyang.energycapstone.common.domain.BillingPeriod
import com.alvinyang.energycapstone.common.domain.Country
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.*

@Entity
@Table(name = "tariff_plan")
class TariffPlan(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val code: String,   // e.g., "SG-RES-FLAT-2026"

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val country: Country,

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false)
    val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,

    @Column(name = "valid_from", nullable = false)
    var validFrom: Instant,

    @Column(name = "valid_to")
    var validTo: Instant? = null,
) {
    @OneToMany(mappedBy = "tariffPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val rates: MutableList<TariffRate> = mutableListOf()

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
}