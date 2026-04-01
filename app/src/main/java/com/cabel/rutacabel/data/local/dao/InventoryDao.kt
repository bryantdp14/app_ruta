package com.cabel.rutacabel.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cabel.rutacabel.data.local.entities.Inventory

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory WHERE routeId = :routeId")
    fun getInventoryByRoute(routeId: Long): LiveData<List<Inventory>>

    @Query("SELECT * FROM inventory WHERE productId = :productId AND routeId = :routeId")
    suspend fun getInventoryByProduct(productId: Long, routeId: Long): Inventory?

    @Query("SELECT * FROM inventory WHERE needsSync = 1")
    suspend fun getInventoryToSync(): List<Inventory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: Inventory): Long

    @Update
    suspend fun updateInventory(inventory: Inventory)

    @Delete
    suspend fun deleteInventory(inventory: Inventory)
}
