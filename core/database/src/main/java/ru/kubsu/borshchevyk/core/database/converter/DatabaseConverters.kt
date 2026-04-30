package ru.kubsu.borshchevyk.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Type converters for Room to handle non-primitive types in the database.
 */
class DatabaseConverters {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Converts a [Set] of strings into a JSON string for database storage.
     * @return JSON string representation of the set, or null if input is null.
     */
    @TypeConverter
    fun fromStringSet(value: Set<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    /**
     * Converts a JSON string back into a [Set] of strings.
     * @return [Set] of strings, or null if input is null.
     */
    @TypeConverter
    fun toStringSet(value: String?): Set<String>? {
        return value?.let { json.decodeFromString(it) }
    }

    /**
     * Converts a [List] of strings into a JSON string for database storage.
     * @return JSON string representation of the list, or null if input is null.
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    /**
     * Converts a JSON string back into a [List] of strings.
     * @return [List] of strings, or null if input is null.
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { json.decodeFromString(it) }
    }
}
