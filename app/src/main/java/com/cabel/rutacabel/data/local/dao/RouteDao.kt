package com.cabel.rutacabel.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cabel.rutacabel.data.local.entities.Route

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes WHERE userId = :userId ORDER BY startDate DESC")
    fun getRoutesByUser(userId: Long): LiveData<List<Route>>

    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: Long): Route?

    @Query("SELECT * FROM routes WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getRouteByRemoteId(remoteId: Long): Route?

    @Query("SELECT * FROM routes WHERE userId = :userId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveRoute(userId: Long): Route?

    @Query("SELECT * FROM routes WHERE needsSync = 1")
    suspend fun getRoutesToSync(): List<Route>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: Route): Long

    @Update
    suspend fun updateRoute(route: Route)

    @Delete
    suspend fun deleteRoute(route: Route)
}
