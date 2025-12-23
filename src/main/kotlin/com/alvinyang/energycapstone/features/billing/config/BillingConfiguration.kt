package com.alvinyang.energycapstone.features.billing.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
class BillingConfiguration(
    private val properties: BillingProperties
) {

    // Define a dedicated thread pool for Billing
    // Limits us to 4 concurrent threads, preventing DB starvation
    @Bean
    fun billingDispatcher(): CoroutineDispatcher {
        // "newFixedThreadPool" creates standard Java threads.
        // "asCoroutineDispatcher" adapts them for Kotlin Coroutines.
        return Executors.newFixedThreadPool(properties.threadPoolSize).asCoroutineDispatcher()
    }
}