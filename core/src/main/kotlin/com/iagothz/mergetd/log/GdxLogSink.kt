package com.iagothz.mergetd.log

import com.badlogic.gdx.Gdx

object GdxLogSink : LogSink {
    override fun write(entry: LogEntry) {
        val app = Gdx.app ?: return StderrLogSink.write(entry)
        val tag = "MergeTD/${entry.module}"
        val line = LogFormat.format(entry.copy(error = null)).substringAfter("] ")
        when (entry.level) {
            LogLevel.DEBUG -> app.debug(tag, line)
            LogLevel.INFO -> app.log(tag, line)
            LogLevel.WARN, LogLevel.ERROR -> if (entry.error != null) app.error(tag, "${entry.level} $line", entry.error) else app.error(tag, "${entry.level} $line")
        }
    }
}
