package com.cabel.rutacabel.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cabel.rutacabel.data.local.entities.Provider

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllProviders(): LiveData<List<Provider>>

    @Query("SELECT * FROM providers WHERE id = :providerId")
    suspend fun getProviderById(providerId: Long): Provider?

    @Query("SELECT * FROM providers WHERE needsSync = 1")
    suspend fun getProvidersToSync(): List<Provider>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: Provider): Long

    @Update
    suspend fun updateProvider(provider: Provider)

    @Delete
    suspend fun deleteProvider(provider: Provider)

    @Query("UPDATE providers SET needsSync = 1 WHERE id = :providerId")
    suspend fun markForSync(providerId: Long)
}
