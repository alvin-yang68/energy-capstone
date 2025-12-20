package com.alvinyang.energycapstone.features.pricing.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

@Converter
class RateConfigurationConverter : AttributeConverter<RateConfiguration, String> {
    // Global singleton mapper is fine for this scale
    private val mapper: ObjectMapper = JsonMapper.builder()
        .findAndAddModules()    // Automatically adds JavaTimeModule & KotlinModule from classpath
        .build()

    override fun convertToDatabaseColumn(attribute: RateConfiguration?): String? {
        if (attribute == null) return null

        return try {
            mapper.writeValueAsString(attribute)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error converting config to JSON", e)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): RateConfiguration? {
        if (dbData.isNullOrEmpty()) return null

        return try {
            // Jackson reads the "type" field and picks the right subclass automatically
            mapper.readValue(dbData, RateConfiguration::class.java)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error converting JSON to config", e)
        }
    }
}