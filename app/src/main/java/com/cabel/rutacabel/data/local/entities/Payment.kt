package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long,
    val userId: Long,
    val routeId: Long,
    val paymentDate: Long = System.currentTimeMillis(),
    val amount: Double,
    val paymentMethod: String = "CASH",
    val reference: String = "",
    val notes: String = "",
    val ticketPrinted: Boolean = false,
    val lastSync: Long = 0,
    val needsSync: Boolean = true,
    val remoteId: Long? = null
)
