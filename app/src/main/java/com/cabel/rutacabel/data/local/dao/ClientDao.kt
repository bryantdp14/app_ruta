package com.cabel.rutacabel.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cabel.rutacabel.data.local.entities.Client

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE isActive = 1 ORDER BY routeOrder ASC")
    fun getAllClients(): LiveData<List<Client>>

    @Query("SELECT * FROM clients WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllClientsList(): List<Client>

    @Query("SELECT * FROM clients WHERE id = :clientId")
    suspend fun getClientById(clientId: Long): Client?

    @Query("SELECT * FROM clients WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getClientByRemoteId(remoteId: Long): Client?

    @Query("SELECT * FROM clients WHERE name LIKE '%' || :query || '%' AND isActive = 1")
    suspend fun searchClients(query: String): List<Client>

    @Query("SELECT * FROM clients WHERE needsSync = 1")
    suspend fun getClientsToSync(): List<Client>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<Client>)

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    @Query("SELECT COUNT(*) FROM clients WHERE needsSync = 1")
    suspend fun getPendingSyncCount(): Int

    @Query("SELECT COUNT(*) FROM clients WHERE isActive = 1")
    suspend fun getTotalCount(): Int

    @Query("UPDATE clients SET needsSync = 1 WHERE id = :clientId")
    suspend fun markForSync(clientId: Long)
}
