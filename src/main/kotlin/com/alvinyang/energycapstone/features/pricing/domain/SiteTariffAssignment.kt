package com.alvinyang.energycapstone.features.pricing.domain

import com.alvinyang.energycapstone.features.customer.domain.Site
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.*

@Entity
@Table(name = "site_tariff_assignment")
class SiteTariffAssignment(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: Site,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_plan_id", nullable = false)
    val tariffPlan: TariffPlan,

    @Column(name = "effective_from", nullable = false)
    val effectiveFrom: Instant,

    @Column(name = "effective_to", nullable = false)
    val effectiveTo: Instant? = null,
) {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
}