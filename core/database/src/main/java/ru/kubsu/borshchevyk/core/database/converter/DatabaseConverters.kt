package ru.kubsu.borshchevyk.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kubsu.borshchevyk.core.model.domain.Attachment
import ru.kubsu.borshchevyk.core.model.domain.MessageReaction
import ru.kubsu.borshchevyk.core.model.domain.User

class DatabaseConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringSet(value: Set<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringSet(value: String?): Set<String>? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromUser(value: User?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toUser(value: String?): User? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromMessageReactions(value: List<MessageReaction>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toMessageReactions(value: String?): List<MessageReaction>? {
        return value?.let { json.decodeFromString(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromAttachments(value: List<Attachment>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toAttachments(value: String?): List<Attachment>? {
        return value?.let { json.decodeFromString(it) } ?: emptyList()
    }
}
