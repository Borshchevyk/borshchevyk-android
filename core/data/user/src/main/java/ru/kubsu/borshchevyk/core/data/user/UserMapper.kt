package ru.kubsu.borshchevyk.core.data.user

import ru.kubsu.borshchevyk.core.database.entity.UserEntity
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.model.dto.UserProfileResponse

fun UserProfileResponse.toEntity(): UserEntity = UserEntity(
    userId = userId,
    email = email,
    tag = tag,
    firstName = firstName,
    lastName = lastName,
    bio = bio,
    avatarUrl = avatarUrl,
    avatars = avatars
)

fun UserEntity.toDomain(): User = User(
    userId = userId,
    email = email,
    tag = tag,
    firstName = firstName,
    lastName = lastName,
    bio = bio,
    avatarUrl = avatarUrl,
    avatars = avatars
)

fun User.toEntity(): UserEntity = UserEntity(
    userId = userId,
    email = email,
    tag = tag,
    firstName = firstName,
    lastName = lastName,
    bio = bio,
    avatarUrl = avatarUrl,
    avatars = avatars
)
