package com.cabel.rutacabel.services

import java.text.SimpleDateFormat
import java.util.*

data class TicketItem(
    val quantity: Int,
    val code: String,
    val description: String,
    val unitPrice: Double,
    val total: Double
)

data class TicketData(
    val companyName: String = "DISTRIBUIDORA CABEL",
    val orderId: Long,
    val orderDate: Long,
    val clientName: String,
    val sellerName: String,
    val items: List<TicketItem>,
    val subtotal: Double,
    val total: Double,
    val paymentMethod: String,
    val creditLimit: Double = 0.0,
    val creditAvailable: Double = 0.0,
    val isCredit: Boolean = false
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(orderDate))
    }

    fun getFolio(): String {
        return String.format("#%04d", orderId)
    }
}
