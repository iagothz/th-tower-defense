package com.iagothz.mergetd.log

import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord

class FileLogSink(directory: File, maxBytes: Int = 1_000_000, files: Int = 5) : LogSink {
    private val handler: FileHandler

    init {
        directory.mkdirs()
        handler = FileHandler(File(directory, "merge-td.%g.log").path, maxBytes, files, true).apply {
            encoding = Charsets.UTF_8.name()
            formatter = object : Formatter() {
                override fun format(record: LogRecord): String = record.message + "\n"
            }
        }
    }

    @Synchronized
    override fun write(entry: LogEntry) {
        handler.publish(LogRecord(Level.INFO, LogFormat.format(entry)))
        handler.flush()
    }

    fun close() = handler.close()
}
