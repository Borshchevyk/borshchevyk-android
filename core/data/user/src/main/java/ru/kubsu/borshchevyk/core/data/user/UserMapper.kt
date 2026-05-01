package ru.kubsu.borshchevyk.core.data.user

import ru.kubsu.borshchevyk.core.database.entity.UserEntity
import ru.kubsu.borshchevyk.core.model.domain.User
import ru.kubsu.borshchevyk.core.network.dto.UserProfileResponse

/**
 * Maps a network [UserProfileResponse] DTO to a local [UserEntity].
 *
 * @return The mapped database entity.
 */
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

/**
 * Maps a local [UserEntity] to a domain [User] model.
 *
 * @return The mapped domain model.
 */
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

/**
 * Maps a domain [User] model back to a local [UserEntity].
 *
 * @return The mapped database entity.
 */
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