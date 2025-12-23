package com.alvinyang.energycapstone.features.billing.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "invoice_line")
class InvoiceLine(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore // Stops recursion when pretty printing as JSON string
    val invoice: Invoice,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false)
    val quantity: BigDecimal,

    @Column(name = "unit_price")
    val unitPrice: BigDecimal?,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    val index: Int
)