package com.iagothz.mergetd.log

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(val epochMillis: Long, val level: LogLevel, val module: String, val message: String, val context: Map<String, Any?>, val error: Throwable?)

fun interface LogSink {
    fun write(entry: LogEntry)
}

class TeeLogSink(private val sinks: List<LogSink>) : LogSink {
    override fun write(entry: LogEntry) = sinks.forEach { it.write(entry) }
}

object StderrLogSink : LogSink {
    override fun write(entry: LogEntry) = System.err.println(LogFormat.format(entry))
}

object Log {
    @Volatile
    var sink: LogSink = StderrLogSink

    @Volatile
    var minLevel: LogLevel = LogLevel.INFO

    fun debug(module: String, message: String, context: Map<String, Any?> = emptyMap(), error: Throwable? = null) = emit(LogLevel.DEBUG, module, message, context, error)

    fun info(module: String, message: String, context: Map<String, Any?> = emptyMap(), error: Throwable? = null) = emit(LogLevel.INFO, module, message, context, error)

    fun warn(module: String, message: String, context: Map<String, Any?> = emptyMap(), error: Throwable? = null) = emit(LogLevel.WARN, module, message, context, error)

    fun error(module: String, message: String, context: Map<String, Any?> = emptyMap(), error: Throwable? = null) = emit(LogLevel.ERROR, module, message, context, error)

    @Suppress("TooGenericExceptionCaught")
    private fun emit(level: LogLevel, module: String, message: String, context: Map<String, Any?>, error: Throwable?) {
        if (level < minLevel) return
        val entry = LogEntry(System.currentTimeMillis(), level, module, message, context, error)
        try {
            sink.write(entry)
        } catch (e: RuntimeException) {
            StderrLogSink.write(entry)
            StderrLogSink.write(LogEntry(entry.epochMillis, LogLevel.ERROR, "log", "Log sink failed", emptyMap(), e))
        }
    }
}
