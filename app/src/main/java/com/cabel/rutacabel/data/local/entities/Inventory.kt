package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class Inventory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val routeId: Long,
    val loadedQuantity: Int,
    val currentQuantity: Int,
    val soldQuantity: Int = 0,
    val returnedQuantity: Int = 0,
    val loadDate: Long = System.currentTimeMillis(),
    val lastSync: Long = 0,
    val needsSync: Boolean = true,
    val remoteId: Long? = null
)
