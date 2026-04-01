package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val authToken: String = "",
    val fullName: String,
    val email: String? = null,
    val roleId: Int = 0,
    val role: String,
    val branchId: Int? = null,
    val phone: String? = null,
    val address: String? = null,
    val photoUrl: String? = null,
    val isActive: Boolean = true,
    val lastSync: Long = 0,
    val needsSync: Boolean = false
)
