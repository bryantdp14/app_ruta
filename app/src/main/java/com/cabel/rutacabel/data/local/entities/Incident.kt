package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class Incident(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long,
    val userId: Long,
    val routeId: Long,
    val incidentDate: Long = System.currentTimeMillis(),
    val incidentType: String,
    val description: String,
    val photoPath: String = "",
    val status: String = "PENDING",
    val resolution: String = "",
    val lastSync: Long = 0,
    val needsSync: Boolean = true,
    val remoteId: Long? = null
)
