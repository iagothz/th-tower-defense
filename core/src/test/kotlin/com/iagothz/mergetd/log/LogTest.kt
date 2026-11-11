package com.iagothz.mergetd.log

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogTest {
    private val entries = mutableListOf<LogEntry>()
    private lateinit var previousSink: LogSink
    private lateinit var previousLevel: LogLevel

    @BeforeTest
    fun capture() {
        previousSink = Log.sink
        previousLevel = Log.minLevel
        Log.sink = LogSink { entries += it }
        Log.minLevel = LogLevel.INFO
    }

    @AfterTest
    fun restore() {
        Log.sink = previousSink
        Log.minLevel = previousLevel
    }

    @Test
    fun dropsEntriesBelowTheMinimumLevel() {
        Log.debug("test", "hidden")
        Log.info("test", "shown")
        Log.minLevel = LogLevel.DEBUG
        Log.debug("test", "now shown")
        assertEquals(listOf("shown", "now shown"), entries.map { it.message })
    }

    @Test
    fun keepsModuleContextAndError() {
        val boom = IllegalStateException("boom")
        Log.error("cloud", "write failed", mapOf("code" to 7), boom)
        val e = entries.single()
        assertEquals(LogLevel.ERROR, e.level)
        assertEquals("cloud", e.module)
        assertEquals(mapOf<String, Any?>("code" to 7), e.context)
        assertEquals(boom, e.error)
    }

    @Test
    fun formatsOneStructuredLineWithTheStackTraceBelow() {
        val entry = LogEntry(0L, LogLevel.WARN, "match", "rejected", mapOf("chars" to 12, "why" to "bad header"), IllegalArgumentException("x"))
        val lines = LogFormat.format(entry).lines()
        assertEquals("1970-01-01T00:00:00.000Z WARN [match] rejected chars=12 why=\"bad header\"", lines.first())
        assertTrue(lines.any { it.contains("IllegalArgumentException: x") })
        assertTrue(lines.any { it.trimStart().startsWith("at ") })
    }

    @Test
    fun aBrokenSinkNeverReachesTheCaller() {
        Log.sink = LogSink { throw IllegalStateException("disk full") }
        Log.error("test", "still fine")
    }

    @Test
    fun teeWritesToEverySink() {
        val other = mutableListOf<LogEntry>()
        Log.sink = TeeLogSink(listOf(LogSink { entries += it }, LogSink { other += it }))
        Log.warn("test", "both")
        assertEquals(1, entries.size)
        assertEquals(1, other.size)
    }
}
