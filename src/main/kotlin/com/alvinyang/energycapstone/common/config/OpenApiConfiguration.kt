package com.alvinyang.energycapstone.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI().info(
        Info()
            .title("Energy Capstone API")
            .version("1.0.0")
            .description("API for Managing Customers, Sites, Tariffs, and Billing.")
    )
}