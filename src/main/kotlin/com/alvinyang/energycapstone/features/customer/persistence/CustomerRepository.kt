package com.alvinyang.energycapstone.features.customer.persistence

import com.alvinyang.energycapstone.features.customer.domain.Customer
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CustomerRepository : JpaRepository<Customer, UUID> {
    fun existsByEmail(email: String): Boolean
}