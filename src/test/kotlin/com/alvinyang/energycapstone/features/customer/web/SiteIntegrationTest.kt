package com.alvinyang.energycapstone.features.customer.web

import com.alvinyang.energycapstone.TestcontainersConfiguration
import com.alvinyang.energycapstone.common.domain.Country
import com.alvinyang.energycapstone.features.customer.domain.Customer
import com.alvinyang.energycapstone.features.customer.domain.Site
import com.alvinyang.energycapstone.features.customer.persistence.CustomerRepository
import com.alvinyang.energycapstone.features.customer.persistence.SiteRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration::class)
class SiteIntegrationTest {
    @Autowired
    lateinit var client: RestTestClient

    @Autowired
    lateinit var customerRepository: CustomerRepository

    @Autowired
    lateinit var siteRepository: SiteRepository

    @BeforeEach
    fun setup() {
        siteRepository.deleteAll()
        customerRepository.deleteAll()
    }

    @Test
    fun `should create site for existing customer`() {
        val customer = customerRepository.save(
            Customer(name = "Test Customer", email = "site@test.com", country = Country.SG)
        )

        val request = CreateSiteRequest(
            customerId = customer.id,
            identifier = "1234567890",
            country = Country.SG,
            region = "SG-NORTH",
            address = "123 Woodlands Ave"
        )

        val responseBody = client.post()
            .uri("/api/sites")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus().isCreated   // Assert 201 Created
            .expectBody(SiteResponse::class.java)   // Deserialize body
            .returnResult()
            .responseBody!!

        assertThat(responseBody.customerId).isEqualTo(customer.id)
        assertThat(responseBody.identifier).isEqualTo("1234567890")

        val inDb = siteRepository.findById(responseBody.id).orElseThrow()
        assertThat(inDb.address).isEqualTo("123 Woodlands Ave")
    }

    @Test
    fun `should fail when customer does not exist`() {
        // Given
        val request = CreateSiteRequest(
            customerId = java.util.UUID.randomUUID(),
            identifier = "999999",
            country = Country.AU,
            region = "AU-NSW",
            address = "Sydney"
        )

        // When & Then
        client.post()
            .uri("/api/sites")
            .body(request)
            .exchange()
            // 1. Assert Status (404 Not Found)
            .expectStatus().isNotFound()
            // 2. Assert it returns a ProblemDetail (RFC 7807)
            .expectBody(ProblemDetail::class.java)
            .value { problem ->
                assertThat(problem?.title).isEqualTo("Resource Not Found")
                assertThat(problem?.detail).contains("Customer not found")
                // If you set the stable type URI:
                assertThat(problem?.type.toString()).contains("errors/not-found")
            }
    }

    @Test
    fun `should fail when site identifier is duplicate`() {
        // Given: A customer and an existing site
        val customer = customerRepository.save(
            Customer(name = "Flo Energy", email = "test@flo.sg", country = Country.SG)
        )

        siteRepository.save(
            Site(
                customer = customer,
                identifier = "13579",
                country = Country.SG,
                region = "SG-NORTH"
            )
        )

        // When: We try to create ANOTHER site with the same identifier + country
        val request = CreateSiteRequest(
            customerId = customer.id,
            identifier = "13579", // Duplicate!
            country = Country.SG,    // Duplicate!
            region = "SG-WEST",
            address = "Different Address"
        )

        // Then: Expect 409 Conflict
        client.post()
            .uri("/api/sites")
            .body(request)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT) // or .is4xxClientError if you prefer generic
            .expectBody(ProblemDetail::class.java)
            .value { problem ->
                assertThat(problem?.title).isEqualTo("Duplicate Resource")
                assertThat(problem?.detail).contains("13579")
                assertThat(problem?.detail).contains("already exists")
            }
    }
}