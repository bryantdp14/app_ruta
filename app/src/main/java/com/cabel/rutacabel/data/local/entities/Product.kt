package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "products",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val barcode: String = "",
    val name: String,
    val description: String = "",
    val category: String = "",
    val categoryId: Int = 0, // New
    val presentationId: Int = 0, // New
    val unitId: Int = 0, // New
    val basePrice: Double,
    val negotiatedPrice: Double = basePrice,
    val stock: Int = 0,
    val minStock: Int = 0,
    val maxStock: Int = 0, // New
    val unit: String = "PZA",
    val imageUrl: String = "",
    val sucursalId: Int = 0, // New
    val codeSat: String = "", // New
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSync: Long = 0,
    val needsSync: Boolean = false,
    val remoteId: Long? = null
)
