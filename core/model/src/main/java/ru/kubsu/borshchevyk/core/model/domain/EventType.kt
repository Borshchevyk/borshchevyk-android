package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Types of synchronization events supported by the sync architecture.
 */
@Serializable
enum class EventType {
    MESSAGE_CREATED,
    MESSAGE_DELETED,
    MESSAGE_UPDATED,
    USER_REGISTERED,
    CHAT_CREATED,
    CHAT_UPDATED,
    CHAT_DELETED,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    MEMBER_UPDATED,
    USER_UPDATED,
    USER_DELETED,
    CALL_EVENT,
    MESSAGE_READ,
    CONTACT_ADDED,
    CONTACT_REMOVED,
    CONTACT_UPDATED,
    PRIVACY_SETTINGS_UPDATED
}
