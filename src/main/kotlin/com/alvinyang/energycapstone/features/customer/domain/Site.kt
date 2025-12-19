package com.alvinyang.energycapstone.features.customer.domain

import com.alvinyang.energycapstone.common.domain.Country
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "site",
    // Mapping the unique constraint we defined in SQL helps Hibernate understand errors
    uniqueConstraints = [UniqueConstraint(columnNames = ["identifier", "country"])]
)
class Site(
    @Column(nullable = false)
    val identifier: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    val country: Country,

    @Column(nullable = false)
    var region: String,

    @Column(columnDefinition = "TEXT")
    var address: String? = null,

    // Many Sites belong to One Customer
    // FetchType.LAZY is MANDATORY for performance. Default is EAGER (bad).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    val customer: Customer,

    @Id
    val id: UUID = UUID.randomUUID(),
) {
    @Column(nullable = false)
    var active: Boolean = true

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Site) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Site(id=$id, identifier='$identifier', country=$country)"
}