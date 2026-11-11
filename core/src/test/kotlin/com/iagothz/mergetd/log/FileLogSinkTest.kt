package com.iagothz.mergetd.log

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class FileLogSinkTest {
    @Test
    fun writesLinesAndRotatesWhenTheFileIsFull() {
        val dir = Files.createTempDirectory("merge-td-logs").toFile()
        val sink = FileLogSink(dir, maxBytes = 2_000, files = 3)
        try {
            repeat(200) { sink.write(LogEntry(0L, LogLevel.INFO, "test", "line $it", emptyMap(), null)) }
        } finally {
            sink.close()
        }
        val logs = dir.listFiles { f -> f.name.endsWith(".log") }.orEmpty()
        assertTrue(logs.size in 2..3, logs.joinToString { it.name })
        assertTrue(logs.all { it.length() <= 2_200 })
        assertTrue(logs.any { it.readText().contains("INFO [test] line 199") })
    }
}
