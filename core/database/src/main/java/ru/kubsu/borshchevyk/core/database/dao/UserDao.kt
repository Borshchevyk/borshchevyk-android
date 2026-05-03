package ru.kubsu.borshchevyk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.kubsu.borshchevyk.core.database.entity.UserEntity

/**
 * Data Access Object (DAO) for handling [UserEntity] operations within the Borshchevyk messenger.
 *
 * Manages user profile data locally, supporting both global server profiles and
 * locally discovered peers in P2P mesh networks.
 */
@Dao
interface UserDao {
    /**
     * Observes a user by their unique ID.
     *
     * @param userId The unique identifier of the user.
     * @return A [Flow] emitting the user entity, or null if not found.
     */
    @Query("SELECT * FROM users WHERE userId = :userId")
    fun observeUser(userId: String): Flow<UserEntity?>

    /**
     * Retrieves a user by their unique ID.
     *
     * @param userId The unique identifier of the user.
     * @return The [UserEntity] if found, otherwise null.
     */
    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUser(userId: String): UserEntity?

    /**
     * Inserts or updates a list of users in the database.
     *
     * @param users The list of [UserEntity] to upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertUsers(users: List<UserEntity>)

    /**
     * Inserts or updates a single user in the database.
     *
     * @param user The [UserEntity] to upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertUser(user: UserEntity)

    /**
     * Deletes a user from the database by their unique ID.
     *
     * @param userId The unique identifier of the user to delete.
     */
    @Query("DELETE FROM users WHERE userId = :userId")
    fun deleteUser(userId: String)

    /**
     * Deletes all users from the database.
     */
    @Query("DELETE FROM users")
    fun deleteAll()
}
