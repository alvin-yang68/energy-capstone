package com.alvinyang.energycapstone.features.billing.domain

import com.alvinyang.energycapstone.common.domain.Currency
import com.alvinyang.energycapstone.features.customer.domain.Customer
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "invoice")
class Invoice(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    val customer: Customer,

    @Column(name = "billing_period_start", nullable = false)
    val billingPeriodStart: Instant,

    @Column(name = "billing_period_end", nullable = false)
    val billingPeriodEnd: Instant,

    @Column(name = "due_date", nullable = false)
    val dueDate: Instant,

    @Column(name = "total_amount", nullable = false)
    var totalAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    val currency: Currency,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: InvoiceStatus = InvoiceStatus.DRAFT,
) {
    @OneToMany(mappedBy = "invoice", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("index ASC")   // Order the list by the 'index' column when fetching
    val lines: MutableList<InvoiceLine> = mutableListOf()

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    var createdAt: Instant? = null

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: Instant? = null
}