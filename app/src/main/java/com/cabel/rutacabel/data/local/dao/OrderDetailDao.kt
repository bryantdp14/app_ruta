package com.cabel.rutacabel.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cabel.rutacabel.data.local.entities.OrderDetail

@Dao
interface OrderDetailDao {
    @Query("SELECT * FROM order_details WHERE orderId = :orderId")
    suspend fun getDetailsByOrder(orderId: Long): List<OrderDetail>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<OrderDetail>)
}
