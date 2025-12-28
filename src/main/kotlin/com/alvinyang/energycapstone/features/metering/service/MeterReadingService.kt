package com.alvinyang.energycapstone.features.metering.service

import com.alvinyang.energycapstone.features.customer.persistence.SiteRepository
import com.alvinyang.energycapstone.features.metering.domain.MeterReading
import com.alvinyang.energycapstone.features.metering.domain.MeterReadingKey
import com.alvinyang.energycapstone.features.metering.persistence.MeterReadingRepository
import com.alvinyang.energycapstone.features.metering.web.IngestReadingRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MeterReadingService(
    private val readingRepo: MeterReadingRepository,
    private val siteRepo: SiteRepository,
) {
    @Transactional
    fun ingestBulk(readings: List<IngestReadingRequest>) {
        if (readings.isEmpty()) return

        // 1. Optimize Site Fetching (Batch Fetch)
        val distinctSiteIds = readings.map { it.siteId }.distinct()
        val sites = siteRepo.findAllById(distinctSiteIds).associateBy { it.id }

        // 2. Map DTOs to Entities
        val entities = readings.mapNotNull { request ->
            val site = sites[request.siteId]

            // If site doesn't exist, we skip this reading (or throw, depending on policy)
            // For bulk, skipping/logging invalid rows is often safer than failing the whole batch
            if (site == null) {
                // Logger.warn("Site ${request.siteId} not found, skipping reading")
                null
            } else {
                MeterReading(
                    id = MeterReadingKey(siteId = site.id, readAt = request.readAt),
                    kwh = request.kwh,
                    site = site,
                )
            }
        }

        readingRepo.saveAll(entities)
    }
}