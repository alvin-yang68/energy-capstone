package com.alvinyang.energycapstone

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<EnergyCapstoneApplication>().with(TestcontainersConfiguration::class).run(*args)
}
