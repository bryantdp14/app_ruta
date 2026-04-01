package com.cabel.rutacabel.data.local.dao

import androidx.room.*
import com.cabel.rutacabel.data.local.entities.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username AND isActive = 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)
    
    @Query("UPDATE users SET authToken = :token WHERE id = :userId")
    suspend fun updateAuthToken(userId: Long, token: String)
}
