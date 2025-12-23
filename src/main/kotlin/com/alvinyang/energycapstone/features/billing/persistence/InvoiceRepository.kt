package com.alvinyang.energycapstone.features.billing.persistence

import com.alvinyang.energycapstone.features.billing.domain.Invoice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface InvoiceRepository : JpaRepository<Invoice, UUID> {
    @Query(
        """
        SELECT COUNT(i) > 0
        FROM Invoice i
        WHERE i.customer.id = :customerId
            AND i.status != 'VOID'
            AND i.billingPeriodStart < :end
            AND i.billingPeriodEnd > :start
    """
    )
    fun existsActiveOverlappingInvoice(customerId: UUID, start: Instant, end: Instant): Boolean
}