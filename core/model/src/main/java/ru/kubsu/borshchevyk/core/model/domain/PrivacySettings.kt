package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

@Serializable
enum class Visibility {
    EVERYONE,
    CONTACTS,
    NOBODY
}

@Serializable
data class PrivacySettings(
    val userId: String,
    val emailVisibility: Visibility = Visibility.NOBODY,
    val searchByEmailVisibility: Visibility = Visibility.EVERYONE,
    val profilePhotoVisibility: Visibility = Visibility.EVERYONE,
    val inviteToChatVisibility: Visibility = Visibility.EVERYONE
)
