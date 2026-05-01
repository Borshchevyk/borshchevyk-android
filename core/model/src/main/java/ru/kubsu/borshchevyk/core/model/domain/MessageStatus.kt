package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Defines the various states of a message's delivery lifecycle.
 */
@Serializable
enum class MessageStatus {
    /** The client is currently attempting to send the message. */
    SENDING,
    /** The server has received the message but it hasn't reached the recipient. */
    RECEIVED_BY_SERVER,
    /** The recipient's device has received the message but the user hasn't opened it. */
    RECEIVED_BY_USER,
    /** The recipient has opened the chat and seen the message. */
    READ,
    /** An error occurred while attempting to send the message. */
    ERROR
}
