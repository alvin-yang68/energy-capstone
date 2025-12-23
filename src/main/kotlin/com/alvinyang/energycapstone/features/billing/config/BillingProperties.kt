package com.alvinyang.energycapstone.features.billing.config

import org.springframework.boot.context.properties.ConfigurationProperties

// Maps to keys starting with "billing.job" in application.yml
// e.g. billing.job.page-size=50
@ConfigurationProperties(prefix = "billing.job")
data class BillingProperties(
    val pageSize: Int = 100,
    val threadPoolSize: Int = 4,
)
