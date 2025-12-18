package com.alvinyang.energycapstone.features.customer.service

import com.alvinyang.energycapstone.common.domain.DuplicateEntityException
import com.alvinyang.energycapstone.features.customer.domain.Customer
import com.alvinyang.energycapstone.features.customer.persistence.CustomerRepository
import com.alvinyang.energycapstone.features.customer.web.CreateCustomerRequest
import com.alvinyang.energycapstone.features.customer.web.CustomerResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomerService(
    private val customerRepository: CustomerRepository
) {
    @Transactional
    fun createCustomer(request: CreateCustomerRequest): CustomerResponse {
        if (customerRepository.existsByEmail(request.email)) {
            throw DuplicateEntityException("Customer with email ${request.email} already exists")
        }

        val entity = Customer(
            name = request.name,
            email = request.email,
            country = request.country
        )

        val saved = customerRepository.save(entity)

        return CustomerResponse(
            id = saved.id,
            name = saved.name,
            email = saved.email,
            country = saved.country,
            createdAt = saved.createdAt?.toString()
                ?: throw IllegalStateException("Entity saved but creation time is missing")
        )
    }
}