package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long,
    val userId: Long,
    val branchId: Int,
    val orderDate: Long = System.currentTimeMillis(),
    val total: Double,
    val paymentMethodId: Int,
    val status: String = "PENDING", // PENDING, SYNCED
    val clientName: String,
    val clientPhone: String,
    val needsSync: Boolean = true,
    val remoteId: Long? = null
)
