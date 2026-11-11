package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Progression
import java.security.MessageDigest

/**
 * The save as the cloud stores it: one preference per line, `type:key=value`, keys in alphabetical order.
 * Types are `i` (int), `s` (string), `b` (boolean) and `f` (float); in a value, `\` is `\\` and a line break `\n`.
 * Device-only preferences (volumes, language, the match in progress, the sync bookkeeping) never enter a blob.
 */
object CloudBlob {
    /** Preferences that belong to the device, not the account. `sync.*` keys count as well. */
    val LOCAL_KEYS: Set<String> = setOf(
        "soundVolume", "musicVolume", "sound", "music", "vibration", "reduceMotion", "largeText", "language", "match", "matchBest",
    )

    /** Level, games played and the day it was uploaded (null when the blob never carried one), for the conflict screen. */
    data class Summary(val level: Int, val games: Int, val day: Int?)

    fun isLocal(key: String): Boolean = key in LOCAL_KEYS || key.startsWith("sync.")

    fun encode(values: Map<String, Any?>): String = values.entries
        .filter { !isLocal(it.key) }
        .sortedBy { it.key }
        .mapNotNull { (k, v) ->
            val type = when (v) {
                is Int -> 'i'
                is String -> 's'
                is Boolean -> 'b'
                is Float -> 'f'
                else -> return@mapNotNull null
            }
            "$type:$k=${escape(v.toString())}"
        }
        .joinToString("\n")

    /** The typed values of [blob], without local keys; null when any line is malformed. */
    fun decode(blob: String): Map<String, Any?>? {
        val out = LinkedHashMap<String, Any?>()
        for (line in blob.split('\n')) {
            if (line.isEmpty()) continue
            if (line.length < 3 || line[1] != ':') return null
            val eq = line.indexOf('=', 2)
            if (eq < 3) return null
            val key = line.substring(2, eq)
            val raw = unescape(line.substring(eq + 1))
            val value: Any = when (line[0]) {
                'i' -> raw.toIntOrNull() ?: return null
                's' -> raw
                'b' -> when (raw) {
                    "true" -> true
                    "false" -> false
                    else -> return null
                }
                'f' -> raw.toFloatOrNull() ?: return null
                else -> return null
            }
            if (!isLocal(key)) out[key] = value
        }
        return out
    }

    /** SHA-256 of the blob without its `sync.*` lines, so bookkeeping alone never counts as a change. */
    fun hash(blob: String): String {
        val content = blob.split('\n').filter { !it.startsWith("i:sync.") }.joinToString("\n")
        return MessageDigest.getInstance("SHA-256").digest(content.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun version(blob: String): Int? = intLine(blob, "sync.version")

    fun summary(blob: String): Summary = Summary(Progression.level(intLine(blob, "xp") ?: 0), intLine(blob, "total.games") ?: 0, intLine(blob, "sync.at"))

    private fun intLine(blob: String, key: String): Int? {
        val prefix = "i:$key="
        return blob.split('\n').firstOrNull { it.startsWith(prefix) }?.substring(prefix.length)?.toIntOrNull()
    }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n")

    private fun unescape(s: String): String {
        val b = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                b.append(if (s[i + 1] == 'n') '\n' else s[i + 1])
                i += 2
            } else {
                b.append(c)
                i++
            }
        }
        return b.toString()
    }
}
