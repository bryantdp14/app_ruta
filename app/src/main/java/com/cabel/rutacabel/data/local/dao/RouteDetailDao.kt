package com.cabel.rutacabel.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cabel.rutacabel.data.local.entities.RouteDetail

@Dao
interface RouteDetailDao {
    
    @Query("SELECT * FROM route_details WHERE rutaId = :rutaId ORDER BY consecutivo ASC")
    fun getRouteDetails(rutaId: Long): LiveData<List<RouteDetail>>
    
    @Query("SELECT * FROM route_details WHERE estatus = 1 ORDER BY consecutivo ASC")
    fun getActiveRouteDetails(): LiveData<List<RouteDetail>>
    
    @Query("SELECT * FROM route_details WHERE needsSync = 1")
    suspend fun getUnsyncedRouteDetails(): List<RouteDetail>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteDetail(routeDetail: RouteDetail): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routeDetails: List<RouteDetail>)
    
    @Update
    suspend fun updateRouteDetail(routeDetail: RouteDetail)
    
    @Query("UPDATE route_details SET visitado = :visitado, fechaVisita = :fechaVisita WHERE id = :id")
    suspend fun markAsVisited(id: Long, visitado: Boolean, fechaVisita: Long)
    
    @Query("UPDATE route_details SET needsSync = 1 WHERE id = :id")
    suspend fun markForSync(id: Long)
    
    @Query("DELETE FROM route_details WHERE rutaId = :rutaId")
    suspend fun deleteByRoute(rutaId: Long)
    
    @Query("DELETE FROM route_details")
    suspend fun deleteAll()
}
