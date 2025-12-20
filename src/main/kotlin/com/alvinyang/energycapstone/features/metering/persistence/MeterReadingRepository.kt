package com.alvinyang.energycapstone.features.metering.persistence

import com.alvinyang.energycapstone.features.metering.domain.MeterReading
import com.alvinyang.energycapstone.features.metering.domain.MeterReadingKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface MeterReadingRepository : JpaRepository<MeterReading, MeterReadingKey> {

    // Fetch readings for a specific site within a billing period
    // Ordering by time is CRITICAL for calculating "Time of Use" or block tiers accurately
    @Query(
        """
        SELECT mr
        FROM MeterReading mr
        WHERE mr.id.siteId = :siteId
            AND mr.id.readAt >= :start
            AND mr.id.readAt < :end
            ORDER BY mr.id.readAt ASC
    """
    )
    fun findBySiteIdAndDateRange(siteId: UUID, start: Instant, end: Instant): List<MeterReading>
}