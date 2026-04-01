package com.cabel.rutacabel.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val colonia: String = "",
    val municipio: String = "",
    val estado: String = "",
    val codigoPostal: Int = 0,
    val sexo: Int = 0, // 0=No especificado, 1=Masculino, 2=Femenino
    val phone: String,
    val email: String = "",
    val taxId: String = "", // RFC
    val regimen: String = "",
    val esProspecto: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val foto: ByteArray? = null,
    val credito: Boolean = false,
    val creditLimit: Double = 0.0,
    val diasEntrega: String = "",
    val horarioEntrega: String = "",
    val currentBalance: Double = 0.0,
    val routeOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSync: Long = 0,
    val needsSync: Boolean = false,
    val remoteId: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Client

        if (id != other.id) return false
        if (name != other.name) return false
        if (address != other.address) return false
        if (phone != other.phone) return false
        if (email != other.email) return false
        if (foto != null) {
            if (other.foto == null) return false
            if (!foto.contentEquals(other.foto)) return false
        } else if (other.foto != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + phone.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + (foto?.contentHashCode() ?: 0)
        return result
    }
}
