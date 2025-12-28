package com.alvinyang.energycapstone.features.metering.web

import com.alvinyang.energycapstone.features.metering.service.MeterReadingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/readings")
class MeterReadingController(
    private val readingService: MeterReadingService
) {

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.ACCEPTED)    // 202 Accepted is common for ingest
    fun ingest(@Valid @RequestBody requests: List<IngestReadingRequest>) = readingService.ingestBulk(requests)
}