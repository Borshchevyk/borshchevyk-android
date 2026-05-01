package ru.kubsu.borshchevyk.feature.chat.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility for formatting message timestamps with caching to improve performance in lists.
 */
object MessageTimeFormatter {
    private val cache = ConcurrentHashMap<String, String>()
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Formats an ISO-8601 time string into a localized "HH:mm" representation.
     * Caches the formatted results to optimize performance in scrollable lists.
     *
     * @param timeStr The ISO-8601 time string to format.
     * @return The formatted local time string.
     */
    fun format(timeStr: String): String {
        return cache.getOrPut(timeStr) {
            try {
                val cleanStr = timeStr.removeSuffix("Z")
                val ldt = LocalDateTime.parse(cleanStr)
                val instant = ldt.toInstant(ZoneOffset.UTC)
                val localTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                localTime.format(formatter)
            } catch (e: Exception) {
                timeStr.substringAfter("T").substringBeforeLast(":")
            }
        }
    }
}
