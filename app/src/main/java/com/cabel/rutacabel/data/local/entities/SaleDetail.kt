package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_details")
data class SaleDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productCode: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val subtotal: Double,
    val lastSync: Long = 0,
    val needsSync: Boolean = true,
    val remoteId: Long? = null
)
