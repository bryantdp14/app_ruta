package com.cabel.rutacabel.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cabel.rutacabel.data.local.entities.Sale
import com.cabel.rutacabel.data.local.entities.SaleDetail

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE routeId = :routeId ORDER BY saleDate DESC")
    fun getSalesByRoute(routeId: Long): LiveData<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleById(saleId: Long): Sale?

    @Query("SELECT * FROM sales WHERE needsSync = 1")
    suspend fun getSalesToSync(): List<Sale>

    @Query("SELECT SUM(total) FROM sales WHERE routeId = :routeId AND status = 'COMPLETED'")
    suspend fun getTotalSalesByRoute(routeId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Update
    suspend fun updateSale(sale: Sale)

    @Delete
    suspend fun deleteSale(sale: Sale)

    @Query("SELECT COUNT(*) FROM sales WHERE needsSync = 1")
    suspend fun getPendingSyncCount(): Int
}

@Dao
interface SaleDetailDao {
    @Query("SELECT * FROM sale_details WHERE saleId = :saleId")
    suspend fun getDetailsBySale(saleId: Long): List<SaleDetail>

    @Query("SELECT * FROM sale_details WHERE needsSync = 1")
    suspend fun getDetailsToSync(): List<SaleDetail>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: SaleDetail): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<SaleDetail>)

    @Delete
    suspend fun deleteDetail(detail: SaleDetail)
}
