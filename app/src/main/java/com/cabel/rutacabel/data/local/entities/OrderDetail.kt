package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_details")
data class OrderDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long, // Local Order ID
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val total: Double
)
