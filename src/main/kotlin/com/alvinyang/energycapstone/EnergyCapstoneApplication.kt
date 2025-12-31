package com.alvinyang.energycapstone

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class EnergyCapstoneApplication

fun main(args: Array<String>) {
    runApplication<EnergyCapstoneApplication>(*args)
}
