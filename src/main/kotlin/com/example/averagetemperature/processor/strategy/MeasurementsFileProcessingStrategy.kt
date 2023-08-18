package com.example.averagetemperature.processor.strategy

import com.example.averagetemperature.input.TemperatureLine
import com.example.averagetemperature.output.AverageTemperatureRecord
import java.io.File
import java.io.FileInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

sealed interface MeasurementsFileProcessingStrategy {
    fun process(measurementsFile: File): Map<String, List<AverageTemperatureRecord>>
}

object BufferedReaderStrategy : MeasurementsFileProcessingStrategy {
    override fun process(measurementsFile: File): Map<String, List<AverageTemperatureRecord>> {
        return FileInputStream(measurementsFile).use { fis ->
            fis.bufferedReader().useLines {
                it.map { fileLine -> TemperatureLine.parse(fileLine) }
                    .groupBy { temperatureInCityAtGivenTime -> temperatureInCityAtGivenTime.city }
                    .mapValues { temperatures ->
                        temperatures.value
                            .groupBy { temperatureAtGivenTime -> temperatureAtGivenTime.date.year }
                            .mapValues { temperatureAtGivenYear ->
                                temperatureAtGivenYear.value.sumOf(TemperatureLine::temperature).divide(
                                    BigDecimal(temperatureAtGivenYear.value.size), 2, RoundingMode.UP
                                )
                            }
                    }.mapValues { records -> averageTemperatureRecords(records) }
            }
        }
    }
}

object FileReaderStrategy : MeasurementsFileProcessingStrategy {
    override fun process(measurementsFile: File): Map<String, List<AverageTemperatureRecord>> {
        val measurementsPerYearPerCity = mutableMapOf<String, MutableMap<Int, Pair<BigDecimal, Int>>>()
        FileInputStream(measurementsFile).use { fis ->
            Scanner(fis).use {
                while (it.hasNext()) {
                    val parsedLine = TemperatureLine.parse(it.nextLine())
                    measurementsPerYearPerCity.putIfAbsent(parsedLine.city, mutableMapOf())
                    val temperatureMeasurement = measurementsPerYearPerCity[parsedLine.city]!!
                    if (temperatureMeasurement.containsKey(parsedLine.date.year)) {
                        val existingMeasurement = temperatureMeasurement[parsedLine.date.year]!!
                        val updatedMeasurement = existingMeasurement.copy(
                            first = existingMeasurement.first + parsedLine.temperature,
                            second = existingMeasurement.second + 1
                        )
                        temperatureMeasurement.replace(parsedLine.date.year, updatedMeasurement)
                    } else
                        temperatureMeasurement[parsedLine.date.year] = parsedLine.temperature to 1
                }
            }
        }
        return measurementsPerYearPerCity.mapValues { measurementsPerYear ->
            measurementsPerYear.value.mapValues { measurements ->
                measurements.value.let { sumOfMeasurements ->
                    sumOfMeasurements.first.divide(
                        BigDecimal(sumOfMeasurements.second), 2, RoundingMode.UP
                    )
                }
            }
        }.mapValues { averageTemperatureRecords(it) }
    }
}

private fun averageTemperatureRecords(averageTemperatureInCitiesAtGivenYear: Map.Entry<String, Map<Int, BigDecimal>>) =
    averageTemperatureInCitiesAtGivenYear.value.map { averageTemperatureAtGivenYear ->
        AverageTemperatureRecord(
            averageTemperatureAtGivenYear.key,
            averageTemperatureAtGivenYear.value
        )
    }
