package com.alvinyang.energycapstone.features.pricing.persistence

import com.alvinyang.energycapstone.features.pricing.domain.TariffPlan
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TariffPlanRepository : JpaRepository<TariffPlan, UUID>