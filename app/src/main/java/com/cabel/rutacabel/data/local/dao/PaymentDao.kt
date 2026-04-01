package com.cabel.rutacabel.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.lifecycle.LiveData
import com.cabel.rutacabel.data.local.entities.Payment

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE needsSync = 1")
    suspend fun getPaymentsToSync(): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Query("SELECT SUM(amount) FROM payments WHERE routeId = :routeId")
    suspend fun getTotalPaymentsByRoute(routeId: Long): Double?

    @Query("SELECT COUNT(*) FROM payments WHERE needsSync = 1")
    suspend fun getPendingSyncCount(): Int
}
