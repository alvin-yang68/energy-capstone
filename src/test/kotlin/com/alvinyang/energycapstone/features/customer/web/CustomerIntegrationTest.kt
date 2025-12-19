package com.alvinyang.energycapstone.features.customer.web

import com.alvinyang.energycapstone.TestcontainersConfiguration
import com.alvinyang.energycapstone.common.domain.Country
import com.alvinyang.energycapstone.features.customer.persistence.CustomerRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration::class)
class CustomerIntegrationTest {
    @Autowired
    lateinit var client: RestTestClient // Spring's HTTP Client for testing

    @Autowired
    lateinit var customerRepository: CustomerRepository

    @BeforeEach
    fun setup() {
        customerRepository.deleteAll()
    }

    @Test
    fun `should create customer end-to-end`() {
        val request = CreateCustomerRequest(
            name = "Flo Energy",
            email = "test@flo.sg",
            country = Country.SG
        )

        val responseBody = client.post()
            .uri("/api/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange() // Execute request
            .expectStatus().isCreated   // Assert 201 Created
            .expectBody(CustomerResponse::class.java)   // Deserialize body
            .returnResult()
            .responseBody!!

        assertThat(responseBody.name).isEqualTo("Flo Energy")
        assertThat(responseBody.id).isNotNull()

        val inDb = customerRepository.findById(responseBody.id).orElseThrow()
        assertThat(inDb.email).isEqualTo("test@flo.sg")
    }
}