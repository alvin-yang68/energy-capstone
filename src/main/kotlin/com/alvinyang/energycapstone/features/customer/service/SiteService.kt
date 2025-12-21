package com.alvinyang.energycapstone.features.customer.service

import com.alvinyang.energycapstone.common.domain.DuplicateEntityException
import com.alvinyang.energycapstone.common.domain.ResourceNotFoundException
import com.alvinyang.energycapstone.features.customer.domain.Site
import com.alvinyang.energycapstone.features.customer.persistence.CustomerRepository
import com.alvinyang.energycapstone.features.customer.persistence.SiteRepository
import com.alvinyang.energycapstone.features.customer.web.CreateSiteRequest
import com.alvinyang.energycapstone.features.customer.web.SiteResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SiteService(
    private val siteRepository: SiteRepository,
    private val customerRepository: CustomerRepository
) {
    @Transactional
    fun createSite(request: CreateSiteRequest): SiteResponse {
        val customer = customerRepository.findByIdOrNull(request.customerId)
            ?: throw ResourceNotFoundException("Customer not found with ID: ${request.customerId}")

        if (siteRepository.existsByIdentifierAndCountry(request.identifier, request.country)) {
            throw DuplicateEntityException("Site with identifier ${request.identifier} already exists in ${request.country}")
        }

        val site = Site(
            identifier = request.identifier,
            country = request.country,
            region = request.region,
            timezone = request.timezone,
            address = request.address,
            customer = customer
        )

        val saved = siteRepository.saveAndFlush(site)

        return SiteResponse(
            id = saved.id,
            customerId = saved.customer.id, // Accessing ID of a LAZY proxy is safe and doesn't trigger a query
            identifier = saved.identifier,
            region = saved.region,
            timezone = saved.timezone,
            address = saved.address,
            active = saved.active,
            createdAt = saved.createdAt?.toString()
                ?: throw IllegalStateException("Site saved but creation time is missing")
        )
    }
}