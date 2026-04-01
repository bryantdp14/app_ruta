package com.cabel.rutacabel.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.*

/**
 * Bluetooth ESC/POS printer service for 58mm thermal printers.
 * Designed for LOSTRAIN 58mm (48mm print width, 32 chars per line).
 */
class BluetoothPrinterService(private val context: Context) {

    companion object {
        private const val TAG = "BTPrinter"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val LINE_WIDTH = 32 // chars for 58mm paper

        // ESC/POS Commands
        private val ESC_INIT = byteArrayOf(0x1B, 0x40) // Initialize printer
        private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
        private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        private val ESC_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10) // Double height
        private val ESC_NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00) // Normal size
        private val ESC_DOUBLE_SIZE = byteArrayOf(0x1D, 0x21, 0x11) // Double width + height
        private val ESC_FEED_LINES = byteArrayOf(0x1B, 0x64, 0x04) // Feed 4 lines
        private val ESC_CUT = byteArrayOf(0x1D, 0x56, 0x00) // Full cut (if supported)
        private val LF = byteArrayOf(0x0A) // Line feed
    }

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothDevice> {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter = btManager.adapter ?: return emptyList()
        if (!btAdapter.isEnabled) return emptyList()
        return btAdapter.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToPrinter(address: String): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter = btManager.adapter ?: return@withContext false
            val device = btAdapter.getRemoteDevice(address) ?: return@withContext false

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            outputStream = socket?.outputStream
            Log.d(TAG, "Connected to printer: $address")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            disconnect()
            false
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
        outputStream = null
        socket = null
    }

    fun isConnected(): Boolean = socket?.isConnected == true

    suspend fun printOrderTicket(ticket: TicketData): Boolean = withContext(Dispatchers.IO) {
        val os = outputStream
        if (os == null || !isConnected()) {
            Log.e(TAG, "Printer not connected")
            return@withContext false
        }

        try {
            // Initialize
            os.write(ESC_INIT)

            // ═══════ HEADER ═══════
            os.write(ESC_ALIGN_CENTER)
            printLine(os, "=".repeat(LINE_WIDTH))

            os.write(ESC_BOLD_ON)
            os.write(ESC_DOUBLE_HEIGHT)
            printLine(os, ticket.companyName)
            os.write(ESC_NORMAL_SIZE)
            os.write(ESC_BOLD_OFF)

            os.write(ESC_BOLD_ON)
            printLine(os, "NOTA DE PEDIDO")
            os.write(ESC_BOLD_OFF)

            printLine(os, "=".repeat(LINE_WIDTH))

            // ═══════ ORDER INFO ═══════
            os.write(ESC_ALIGN_LEFT)
            printLine(os, formatTwoColumns("Folio: ${ticket.getFolio()}", ticket.getFormattedDate()))
            printLine(os, "Cliente: ${truncate(ticket.clientName, LINE_WIDTH - 9)}")
            printLine(os, "Vendedor: ${truncate(ticket.sellerName, LINE_WIDTH - 10)}")

            if (ticket.isCredit) {
                printLine(os, "Método: Crédito")
                printLine(os, formatTwoColumns(
                    "Límite: $${fmtPrice(ticket.creditLimit)}",
                    "Disp: $${fmtPrice(ticket.creditAvailable)}"
                ))
            } else {
                printLine(os, "Método: Efectivo")
            }

            // ═══════ ITEMS HEADER ═══════
            printLine(os, "-".repeat(LINE_WIDTH))
            os.write(ESC_BOLD_ON)
            // Header: CANT COD    DESCRIPCION  PRECIO
            printLine(os, formatItemHeader())
            os.write(ESC_BOLD_OFF)
            printLine(os, "-".repeat(LINE_WIDTH))

            // ═══════ ITEMS ═══════
            for (item in ticket.items) {
                printItemLine(os, item)
            }

            // ═══════ TOTALS ═══════
            printLine(os, "-".repeat(LINE_WIDTH))
            os.write(ESC_BOLD_ON)
            os.write(ESC_ALIGN_RIGHT)
            printLine(os, "TOTAL: $${fmtPrice(ticket.total)}")
            os.write(ESC_BOLD_OFF)
            os.write(ESC_ALIGN_LEFT)

            // Items count
            val totalItems = ticket.items.sumOf { it.quantity }
            printLine(os, "Artículos: $totalItems")

            // ═══════ FOOTER ═══════
            printLine(os, "=".repeat(LINE_WIDTH))
            os.write(ESC_ALIGN_CENTER)
            os.write(ESC_BOLD_ON)
            printLine(os, "¡Gracias por su compra!")
            os.write(ESC_BOLD_OFF)
            printLine(os, "=".repeat(LINE_WIDTH))

            // Feed and cut
            os.write(ESC_FEED_LINES)
            os.write(ESC_CUT)

            os.flush()
            Log.d(TAG, "Ticket printed successfully")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Print error: ${e.message}")
            disconnect()
            false
        }
    }

    // ──────── Formatting Helpers ────────

    private fun printLine(os: OutputStream, text: String) {
        os.write(text.toByteArray(Charsets.UTF_8))
        os.write(LF)
    }

    private fun formatTwoColumns(left: String, right: String): String {
        val maxLeft = LINE_WIDTH - right.length - 1
        val l = if (left.length > maxLeft) left.substring(0, maxLeft) else left
        val spaces = LINE_WIDTH - l.length - right.length
        return l + " ".repeat(maxOf(1, spaces)) + right
    }

    private fun formatItemHeader(): String {
        return "DESCRIPCION DE PRODUCTOS"
    }

    private fun printItemLine(os: OutputStream, item: TicketItem) {
        // Line 1: CODE - DESCRIPTION
        val line1 = "${item.code} - ${item.description}"
        printLine(os, truncate(line1, LINE_WIDTH))
        
        // Line 2:   QTY x UNIT_PRICE      TOTAL
        val qtyPrice = "  ${item.quantity} x ${fmtPrice(item.unitPrice)}"
        val total = fmtPrice(item.total)
        val spaces = LINE_WIDTH - qtyPrice.length - total.length
        val line2 = qtyPrice + " ".repeat(maxOf(1, spaces)) + total
        printLine(os, line2)
    }

    private fun fmtPrice(value: Double): String {
        return String.format("%.2f", value)
    }

    private fun truncate(text: String, maxLen: Int): String {
        return if (text.length > maxLen) text.substring(0, maxLen) else text
    }
}
