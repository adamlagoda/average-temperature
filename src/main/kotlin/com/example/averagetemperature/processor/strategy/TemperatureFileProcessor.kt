package com.example.averagetemperature.processor.strategy

import com.example.averagetemperature.output.AverageTemperatureRecord
import java.io.File
import kotlin.io.path.Path

class TemperatureFileProcessor(private val filePath: String) {
    private val file: File = Path(filePath).toFile()

    fun process(strategy: MeasurementsFileProcessingStrategy): Map<String, List<AverageTemperatureRecord>> {
        return strategy.process(file)
    }
}