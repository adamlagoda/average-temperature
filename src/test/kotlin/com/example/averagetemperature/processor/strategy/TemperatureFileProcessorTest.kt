package com.example.averagetemperature.processor.strategy

import kotlinx.coroutines.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileOutputStream
import java.math.BigDecimal
import kotlin.io.path.Path
import kotlin.system.measureTimeMillis

internal class TemperatureFileProcessorTest {

    lateinit var processor: TemperatureFileProcessor

    @BeforeEach
    fun setupProcessor() {
        Path(originalFilePath).toFile().let { file ->
            file.copyTo(Path(testFilePath).toFile())
        }
        processor = TemperatureFileProcessor(testFilePath)
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `should generate list of average temperatures`() {
        // given
        val strategy = BufferedReaderStrategy

        // when
        val averageTemperaturesByCity = processor.process(strategy)

        // then - all cities have aggregated measurements
        assertThat(averageTemperaturesByCity).containsKeys("Warszawa", "Wrocław", "Kraków", "Poznań", "Gdańsk", "Łódź")

        // and - check one average temperature point
        assertThat(averageTemperaturesByCity.get("Warszawa")).anySatisfy {
            it.year == 2018 && it.averageTemperature.compareTo(BigDecimal(13.53)) == 0
        }
    }

    @Test
    fun `should generate same results with all strategies`() {
        // given
        val bufferedReaderStrategy = BufferedReaderStrategy
        val fileReaderStrategy = FileReaderStrategy

        // when
        val bufferedReaderResults = processor.process(bufferedReaderStrategy)
        val fileReaderResults = processor.process(fileReaderStrategy)

        // then
        bufferedReaderResults.forEach { entry ->
            val fileReaderEntry = fileReaderResults[entry.key]
            assertThat(entry.value).containsAll(fileReaderEntry)
        }
    }

    @Test
    fun `should execute in similar time`() {
        // given
        val bufferedReaderStrategy = BufferedReaderStrategy
        val fileReaderStrategy = FileReaderStrategy

        // when
        val bufferedReaderExecutionTime = measureTimeMillis {
            processor.process(bufferedReaderStrategy)
        }

        val fileReaderExecutionTime = measureTimeMillis {
            processor.process(fileReaderStrategy)
        }

        // then
        assertThat(bufferedReaderExecutionTime).isCloseTo(fileReaderExecutionTime, Percentage.withPercentage(10.0))
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `should include measurement when file was overwritten during reading`(strategy: MeasurementsFileProcessingStrategy) {
        // when
        val averageTemperaturesByCity =
            CoroutineScope(Dispatchers.IO).async(start = CoroutineStart.LAZY) { processor.process(strategy) }

        // then
        runBlocking {
            averageTemperaturesByCity.start()
            CoroutineScope(Dispatchers.IO).launch { appendLine() }
            val result = averageTemperaturesByCity.await()
            assertThat(result).containsKey("Lublin")
        }
    }

    companion object {
        const val originalFilePath =
            "/Users/adamlagoda/IdeaProjects/average-temperature/src/test/resources/example_file.csv"
        const val testFilePath =
            "/Users/adamlagoda/IdeaProjects/average-temperature/src/test/resources/example_file_copy.csv"
        const val lineToAppend = "Lublin;2018-10-03 03:11:08.871;23.21"

        @JvmStatic
        fun strategies(): List<Arguments> {
            return listOf(Arguments.of(BufferedReaderStrategy), Arguments.of(FileReaderStrategy))
        }
    }

    private fun appendLine() {
        val file = Path(testFilePath).toFile()
        FileOutputStream(file, true).bufferedWriter().use { writer ->
            writer.appendLine(lineToAppend)
        }
    }

    @AfterEach
    fun removeFile() {
        Path(testFilePath).toFile().delete()
    }
}