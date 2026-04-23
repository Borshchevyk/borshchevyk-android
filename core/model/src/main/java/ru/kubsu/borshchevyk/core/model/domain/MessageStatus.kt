package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
enum class MessageStatus {
    RECEIVED_BY_SERVER,
    RECEIVED_BY_USER,
    READ,
    ERROR
}
