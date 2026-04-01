package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long,
    val userId: Long,
    val routeId: Long,
    val saleDate: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val tax: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paymentMethod: String = "CASH",
    val status: String = "PENDING",
    val notes: String = "",
    val ticketPrinted: Boolean = false,
    val lastSync: Long = 0,
    val needsSync: Boolean = true,
    val remoteId: Long? = null
)
