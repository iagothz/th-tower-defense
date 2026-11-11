package com.iagothz.mergetd.log

import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object LogFormat {
    fun format(entry: LogEntry): String = buildString {
        append(timestamp(entry.epochMillis)).append(' ')
        append(entry.level.name).append(" [").append(entry.module).append("] ")
        append(entry.message)
        for ((key, value) in entry.context) append(' ').append(key).append('=').append(value(value))
        entry.error?.let { append('\n').append(stackTrace(it).trimEnd()) }
    }

    private fun timestamp(epochMillis: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(epochMillis))

    private fun value(v: Any?): String {
        val text = v.toString()
        return if (text.any { it.isWhitespace() || it == '"' || it == '=' }) "\"" + text.replace("\"", "\\\"") + "\"" else text
    }

    private fun stackTrace(t: Throwable): String = StringWriter().also { t.printStackTrace(PrintWriter(it)) }.toString()
}
