package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "product_prices",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["remoteId"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productoId"])]
)
data class ProductPrice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remotePrecioId: Long,
    val productoId: Long,
    val idLista: Int,
    val listaPrecio: String,
    val precio: Double,
    val cantMin: Int? = null,
    val cantMax: Int? = null,
    val publico: Boolean = false,
    val lastSync: Long = System.currentTimeMillis()
)
