package com.alvinyang.energycapstone

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun timescaleContainer(): PostgreSQLContainer {
        return PostgreSQLContainer(
            DockerImageName.parse("timescale/timescaledb:latest-pg16").asCompatibleSubstituteFor("postgres")
        )
            .withDatabaseName("energy_db")
            .withUsername("postgres")
            .withPassword("password")
    }
}
