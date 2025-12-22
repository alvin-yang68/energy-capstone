package com.alvinyang.energycapstone.features.pricing.persistence

import com.alvinyang.energycapstone.features.pricing.domain.SiteTariffAssignment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface SiteTariffAssignmentRepository : JpaRepository<SiteTariffAssignment, UUID> {
    @Query("""
        SELECT sta 
        FROM SiteTariffAssignment sta
        WHERE sta.site.id = :siteId
            AND sta.effectiveFrom <= :end
            AND (sta.effectiveTo IS NULL OR sta.effectiveTo >= :start)
        ORDER BY sta.effectiveFrom ASC
    """)
    fun findOverlappingAssignment(siteId: UUID, start: Instant, end: Instant): List<SiteTariffAssignment>
}