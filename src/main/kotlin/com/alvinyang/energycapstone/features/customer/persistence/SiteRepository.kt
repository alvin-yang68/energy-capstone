package com.alvinyang.energycapstone.features.customer.persistence

import com.alvinyang.energycapstone.common.domain.Country
import com.alvinyang.energycapstone.features.customer.domain.Site
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface SiteRepository : JpaRepository<Site, UUID> {
    // Check for uniqueness before creating
    fun existsByIdentifierAndCountry(identifier: String, country: Country): Boolean
}