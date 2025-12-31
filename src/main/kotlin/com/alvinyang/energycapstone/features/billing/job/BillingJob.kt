package com.alvinyang.energycapstone.features.billing.job

import com.alvinyang.energycapstone.common.infrastructure.LogExecutionTime
import com.alvinyang.energycapstone.features.billing.config.BillingProperties
import com.alvinyang.energycapstone.features.billing.service.InvoiceService
import com.alvinyang.energycapstone.features.customer.persistence.CustomerRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Component
class BillingJob(
    private val customerRepository: CustomerRepository,
    private val invoiceService: InvoiceService,

    // Inject our custom dispatcher defined in @Configuration
    @Qualifier("billingDispatcher") private val dispatcher: CoroutineDispatcher,

    private val properties: BillingProperties,
) {
    private val logger = LoggerFactory.getLogger(BillingJob::class.java)

    // Run at 2:00 AM on the 1st day of every month
    // "0 0 2 1 * ?" -> Seconds Minutes Hours DayOfMonth Month DayOfWeek
    @Scheduled(cron = "0 0 2 1 * ?")
    @LogExecutionTime
    fun runMonthlyBilling() {
        // Bridges the Blocking world (Spring Scheduler) to the Suspending world (Coroutines).
        // We pass 'dispatcher' to ensure execution shifts immediately to our
        // custom fixed-thread-pool, protecting the main application threads.
        // This block will not return until all child jobs inside are complete.
        runBlocking(dispatcher) {
            logger.info("Starting monthly billing job...")

            // 1. Determine Billing Period (Previous Month)
            // e.g., If now is Feb 1st, period is Jan 1st 00:00 to Feb 1st 00:00
            val now = ZonedDateTime.now(ZoneId.of("Asia/Singapore"))
            val billingPeriodEnd = now.truncatedTo(ChronoUnit.DAYS).toInstant()
            val billingPeriodStart = now.minusMonths(1).truncatedTo(ChronoUnit.DAYS).toInstant()

            // 2. Pagination Loop (The Batch Process)
            val pageSize = properties.pageSize
            var pageNumber = 0
            var hasNext = true

            while (hasNext) {
                // Fetch a chunk of customers (Standard JPA is blocking, that's fine here)
                val customerPage = customerRepository.findAll(PageRequest.of(pageNumber, pageSize))
                if (customerPage.isEmpty) {
                    hasNext = false
                    continue
                }

                logger.info("Processing page $pageNumber with ${customerPage.numberOfElements} customers")

                // 3. Structured Concurrency (Fan-Out)
                // 'supervisorScope' ensures if one customer fails, others keep going
                supervisorScope {
                    val jobs = customerPage.content.map { customer ->
                        launch {
                            try {
                                invoiceService.generateInvoiceForCustomer(
                                    customerId = customer.id,
                                    billingPeriodStart,
                                    billingPeriodEnd
                                )
                            } catch (e: IllegalStateException) {
                                // Expected behavior for re-runs
                                logger.info("Skipping customer ${customer.id}: ${e.message}")
                            } catch (e: Exception) {
                                // Log but don't crash the whole job
                                logger.error("Failed to bill customer ${customer.id}: ${e.message}")
                            }
                        }
                    }

                    // Wait for this batch to finish before fetching the next page
                    // This controls memory pressure
                    jobs.joinAll()
                }

                if (customerPage.hasNext()) {
                    pageNumber++
                } else {
                    hasNext = false
                }
            }

            logger.info("Monthly billing job completed.")
        }
    }
}