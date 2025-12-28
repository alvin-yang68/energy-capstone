package com.alvinyang.energycapstone.features.pricing.web

import com.alvinyang.energycapstone.features.pricing.service.TariffManagementService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tariffs")
class TariffController(
    private val tariffService: TariffManagementService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPlan(@Valid @RequestBody request: CreateTariffPlanRequest): TariffPlanResponse =
        tariffService.createPlan(request)

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    fun assignPlan(@Valid @RequestBody request: AssignTariffRequest): AssignmentResponse =
        tariffService.assignPlan(request)
}