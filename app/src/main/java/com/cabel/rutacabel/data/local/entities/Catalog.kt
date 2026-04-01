package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalogs")
data class Catalog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "CATEGORY", "UNIT", "PRESENTATION", "PAYMENT_METHOD"
    val remoteId: Int,
    val name: String,
    val value: String = "", // Optional extra data
    val lastSync: Long = 0
)
