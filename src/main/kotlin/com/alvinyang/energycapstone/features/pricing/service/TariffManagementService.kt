package com.alvinyang.energycapstone.features.pricing.service

import com.alvinyang.energycapstone.common.domain.ResourceNotFoundException
import com.alvinyang.energycapstone.features.customer.persistence.SiteRepository
import com.alvinyang.energycapstone.features.pricing.domain.SiteTariffAssignment
import com.alvinyang.energycapstone.features.pricing.domain.TariffPlan
import com.alvinyang.energycapstone.features.pricing.domain.TariffRate
import com.alvinyang.energycapstone.features.pricing.persistence.SiteTariffAssignmentRepository
import com.alvinyang.energycapstone.features.pricing.persistence.TariffPlanRepository
import com.alvinyang.energycapstone.features.pricing.web.AssignTariffRequest
import com.alvinyang.energycapstone.features.pricing.web.AssignmentResponse
import com.alvinyang.energycapstone.features.pricing.web.CreateTariffPlanRequest
import com.alvinyang.energycapstone.features.pricing.web.TariffPlanResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TariffManagementService(
    private val tariffRepo: TariffPlanRepository,
    private val assignmentRepo: SiteTariffAssignmentRepository,
    private val siteRepo: SiteRepository,
) {

    @Transactional
    fun createPlan(request: CreateTariffPlanRequest): TariffPlanResponse {
        // 1. Create Parent
        val plan = TariffPlan(
            code = request.code,
            name = request.name,
            country = request.country,
            billingPeriod = request.billingPeriod,
            validFrom = Instant.now()
        )

        // 2. Add Children (Cascade will save them)
        request.rates.forEach { rateRequest ->
            val rate = TariffRate(
                tariffPlan = plan,
                rateType = rateRequest.rateType,
                description = rateRequest.description,
                configuration = rateRequest.configuration
            )
            plan.rates.add(rate)
        }

        val saved = tariffRepo.save(plan)
        return TariffPlanResponse(saved.id, saved.name)
    }

    @Transactional
    fun assignPlan(request: AssignTariffRequest): AssignmentResponse {
        val site = siteRepo.findByIdOrNull(request.siteId) ?: throw ResourceNotFoundException("Site not found")
        val plan =
            tariffRepo.findByIdOrNull(request.tariffPlanId) ?: throw ResourceNotFoundException("Tariff Plan not found")

        // TODO: In a real app, you would check for overlap conflicts here!
        val assignment = SiteTariffAssignment(
            site = site,
            tariffPlan = plan,
            effectiveFrom = request.effectiveFrom
        )

        val saved = assignmentRepo.save(assignment)
        return AssignmentResponse(saved.id, siteId = site.id, planId = plan.id)
    }
}