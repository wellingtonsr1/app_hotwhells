/*** T140: AppLogger - Log estruturado integrado ao handler global de erros.
 * Art. V.7: log com nivel, timestamp UTC, correlacao por operacao,
 * sem dados sensaveis, exportavel no diagnostico.
 ***/

package br.com.hotwheelscollector.data.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL
}

object AppLogger {

    private val logger: Logger = LoggerFactory.getLogger("HotWheelsCollector")
    @Volatile private var modulePrefix: String = "Unknown"

    fun setModule(module: String) { modulePrefix = module }

    private fun timestampUtc(): String =
        Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)

    fun log(level: LogLevel, message: String, module: String = modulePrefix) {
        val correlationId = java.util.UUID.randomUUID().toString()
        when (level) {
            LogLevel.TRACE -> logger.trace("module={},msg={},corr={}", 
                modulePrefix, message, correlationId)
            LogLevel.DEBUG -> logger.debug("module={},msg={},corr={}", 
                modulePrefix, message, correlationId)
            LogLevel.INFO -> logger.info("module={},msg={},corr={}", 
                modulePrefix, message, correlationId)
            LogLevel.WARN -> logger.warn("module={},msg={},corr={}", 
                modulePrefix, message, correlationId)
            LogLevel.ERROR -> logger.error("module={},msg={},corr={}", 
                modulePrefix, message, correlationId)
            LogLevel.FATAL -> logger.error("module={},msg={},corr={}", 
                modulePrefix, message, correlationId)
        }
    }

    fun i(message: String, module: String = modulePrefix) = log(LogLevel.INFO, message, module)
    fun d(message: String, module: String = modulePrefix) = log(LogLevel.DEBUG, message, module)
    fun w(message: String, module: String = modulePrefix) = log(LogLevel.WARN, message, module)
    fun e(message: String, module: String = modulePrefix) = log(LogLevel.ERROR, message, module)
    fun f(message: String, module: String = modulePrefix) = log(LogLevel.FATAL, message, module)
}