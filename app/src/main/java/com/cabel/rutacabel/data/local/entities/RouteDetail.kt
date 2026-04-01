package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_details")
data class RouteDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rutaId: Long,
    val nombreRuta: String,
    val consecutivo: Int,
    val clienteId: Long,
    val clienteNombre: String,
    val folioSucursal: String? = null,
    val ventaId: Long? = null,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val estatus: Int = 1, // 1=Activa, 0=Inactiva
    val visitado: Boolean = false,
    val fechaVisita: Long? = null,
    val lastSync: Long = 0,
    val needsSync: Boolean = false
)
