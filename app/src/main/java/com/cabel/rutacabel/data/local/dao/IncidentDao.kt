package com.cabel.rutacabel.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cabel.rutacabel.data.local.entities.Incident

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents WHERE routeId = :routeId ORDER BY incidentDate DESC")
    fun getIncidentsByRoute(routeId: Long): LiveData<List<Incident>>

    @Query("SELECT * FROM incidents WHERE clientId = :clientId ORDER BY incidentDate DESC")
    suspend fun getIncidentsByClient(clientId: Long): List<Incident>

    @Query("SELECT * FROM incidents WHERE needsSync = 1")
    suspend fun getIncidentsToSync(): List<Incident>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: Incident): Long

    @Update
    suspend fun updateIncident(incident: Incident)

    @Delete
    suspend fun deleteIncident(incident: Incident)
}
