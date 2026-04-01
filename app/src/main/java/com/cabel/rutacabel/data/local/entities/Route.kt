package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class Route(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val routeName: String,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val status: String = "PENDING",
    val totalSales: Double = 0.0,
    val totalPayments: Double = 0.0,
    val clientsVisited: Int = 0,
    val lastSync: Long = 0,
    val needsSync: Boolean = true,
    val remoteId: Long? = null
)
