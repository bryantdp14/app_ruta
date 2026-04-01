package com.cabel.rutacabel.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cabel.rutacabel.data.local.entities.Order

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE needsSync = 1")
    suspend fun getOrdersToSync(): List<Order>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("UPDATE orders SET needsSync = 0, status = 'SYNCED' WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("SELECT COUNT(*) FROM orders WHERE needsSync = 1")
    suspend fun getPendingSyncCount(): Int
}
