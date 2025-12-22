package com.alvinyang.energycapstone.features.customer.persistence

import com.alvinyang.energycapstone.common.domain.Country
import com.alvinyang.energycapstone.features.customer.domain.Site
import com.alvinyang.energycapstone.features.customer.domain.SitePricingContext
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface SiteRepository : JpaRepository<Site, UUID> {
    // Check for uniqueness before creating
    fun existsByIdentifierAndCountry(identifier: String, country: Country): Boolean

    // Spring Data Magic:
    // It sees the return type 'SitePricingContext'.
    // It checks the constructor properties: 'id', 'timezone'.
    // It generates SQL: SELECT id, timezone FROM site WHERE id = ?
    fun findPricingContextById(id: UUID): SitePricingContext?
}