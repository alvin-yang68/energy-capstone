package com.alvinyang.energycapstone.features.customer.web

import com.alvinyang.energycapstone.features.customer.service.SiteService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sites")
class SiteController(
    private val siteService: SiteService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSite(@Valid @RequestBody request: CreateSiteRequest): SiteResponse {
        return siteService.createSite(request)
    }
}