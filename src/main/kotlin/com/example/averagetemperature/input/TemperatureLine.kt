package com.example.averagetemperature.input

import java.lang.NumberFormatException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.regex.Pattern

data class TemperatureLine(
    internal val city: String,
    internal val date: LocalDate,
    internal val temperature: BigDecimal
) {
    companion object {
        private val delimiterPattern = Pattern.compile("[; ]")
        fun parse(line: String): TemperatureLine {
            val fields = line.split(delimiterPattern)
            try {
                return TemperatureLine(
                    city = fields[0],
                    date = LocalDate.parse(fields[1]),
                    temperature = fields[3].toBigDecimal()
                )
            } catch (e: NumberFormatException) {
                println(line)
            }
            return empty()
        }

        private fun empty(): TemperatureLine {
            return TemperatureLine("", LocalDate.MIN, BigDecimal(0))
        }
    }
}