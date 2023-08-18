package com.example.averagetemperature.output

import java.math.BigDecimal

data class AverageTemperatureRecord(
    internal val year: Int,
    internal val averageTemperature: BigDecimal
) {
}