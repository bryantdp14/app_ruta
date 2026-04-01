package com.cabel.rutacabel.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class PrinterManager(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val TAG = "PrinterManager"
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private const val ESC = 0x1B.toByte()
        private const val GS = 0x1D.toByte()
        private const val LF = 0x0A.toByte()
        private const val CR = 0x0D.toByte()

        private val INIT = byteArrayOf(ESC, '@'.code.toByte())
        private val ALIGN_CENTER = byteArrayOf(ESC, 'a'.code.toByte(), 1)
        private val ALIGN_LEFT = byteArrayOf(ESC, 'a'.code.toByte(), 0)
        private val ALIGN_RIGHT = byteArrayOf(ESC, 'a'.code.toByte(), 2)
        
        private val BOLD_ON = byteArrayOf(ESC, 'E'.code.toByte(), 1)
        private val BOLD_OFF = byteArrayOf(ESC, 'E'.code.toByte(), 0)
        
        private val FONT_SIZE_NORMAL = byteArrayOf(GS, '!'.code.toByte(), 0x00)
        private val FONT_SIZE_DOUBLE = byteArrayOf(GS, '!'.code.toByte(), 0x11)
        private val FONT_SIZE_LARGE = byteArrayOf(GS, '!'.code.toByte(), 0x22)
        
        private val UNDERLINE_ON = byteArrayOf(ESC, '-'.code.toByte(), 1)
        private val UNDERLINE_OFF = byteArrayOf(ESC, '-'.code.toByte(), 0)
        
        private val CUT_PAPER = byteArrayOf(GS, 'V'.code.toByte(), 66, 0)
        private val FEED_LINES = byteArrayOf(ESC, 'd'.code.toByte(), 3)
    }

    fun getPairedPrinters(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) {
            Log.w(TAG, "Missing Bluetooth permission")
            return emptyList()
        }
        
        return bluetoothAdapter?.bondedDevices?.filter { device ->
            device.name?.contains("printer", ignoreCase = true) == true ||
            device.name?.contains("POS", ignoreCase = true) == true ||
            device.name?.contains("PT210", ignoreCase = true) == true ||
            device.name?.contains("MTP", ignoreCase = true) == true ||
            device.name?.contains("GOOJPRT", ignoreCase = true) == true
        } ?: emptyList()
    }
    
    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    suspend fun connectToPrinter(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!hasBluetoothPermission()) {
                Log.e(TAG, "Missing Bluetooth permission")
                return@withContext false
            }
            
            bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to printer", e)
            false
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    suspend fun printSaleTicket(
        clientName: String,
        items: List<TicketItem>,
        subtotal: Double,
        tax: Double,
        total: Double,
        paymentMethod: String
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            outputStream?.apply {
                write(INIT)
                Thread.sleep(50)

                write(ALIGN_CENTER)
                write(FONT_SIZE_LARGE)
                write(BOLD_ON)
                writeLine("RUTA CABEL")
                write(BOLD_OFF)
                write(FONT_SIZE_NORMAL)
                writeLine("Sistema de Ventas en Ruta")
                writeLine("--------------------------------")
                write(ALIGN_LEFT)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                writeLine("Fecha: ${dateFormat.format(Date())}")
                writeLine("Cliente: $clientName")
                writeLine("================================")

                write(BOLD_ON)
                writeLine(String.format("%-18s %4s %6s", "Producto", "Cant", "Total"))
                write(BOLD_OFF)
                writeLine("--------------------------------")

                items.forEach { item ->
                    val productName = if (item.name.length > 18) 
                        item.name.substring(0, 18) 
                    else 
                        item.name
                    
                    writeLine(String.format(
                        "%-18s %4d $%5.2f",
                        productName,
                        item.quantity,
                        item.subtotal
                    ))
                    
                    if (item.unitPrice != item.subtotal / item.quantity) {
                        writeLine("  @$%.2f c/u".format(item.unitPrice))
                    }
                }

                writeLine("================================")
                writeLine(String.format("%-20s $%7.2f", "Subtotal:", subtotal))
                writeLine(String.format("%-20s $%7.2f", "IVA (16%):", tax))
                writeLine("--------------------------------")
                write(FONT_SIZE_DOUBLE)
                write(BOLD_ON)
                writeLine(String.format("%-12s $%7.2f", "TOTAL:", total))
                write(BOLD_OFF)
                write(FONT_SIZE_NORMAL)
                writeLine("================================")
                writeLine("Metodo de pago: $paymentMethod")
                writeLine("")

                write(ALIGN_CENTER)
                write(FONT_SIZE_NORMAL)
                writeLine("Gracias por su compra!")
                writeLine("Vuelva pronto")
                write(FEED_LINES)

                write(CUT_PAPER)
                flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error printing ticket", e)
            false
        }
    }

    suspend fun printPaymentTicket(
        clientName: String,
        amount: Double,
        paymentMethod: String,
        reference: String
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            outputStream?.apply {
                write(INIT)
                Thread.sleep(50)

                write(ALIGN_CENTER)
                write(FONT_SIZE_LARGE)
                write(BOLD_ON)
                writeLine("RUTA CABEL")
                write(BOLD_OFF)
                write(FONT_SIZE_NORMAL)
                writeLine("Recibo de Cobro")
                writeLine("--------------------------------")
                write(ALIGN_LEFT)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                writeLine("Fecha: ${dateFormat.format(Date())}")
                writeLine("Cliente: $clientName")
                writeLine("================================")

                write(ALIGN_CENTER)
                write(FONT_SIZE_LARGE)
                write(BOLD_ON)
                writeLine("MONTO COBRADO")
                write(FONT_SIZE_DOUBLE)
                writeLine(String.format("$%.2f", amount))
                write(BOLD_OFF)
                write(FONT_SIZE_NORMAL)
                writeLine("================================")
                write(ALIGN_LEFT)
                
                writeLine("Metodo: $paymentMethod")
                if (reference.isNotEmpty()) {
                    writeLine("Referencia: $reference")
                }
                writeLine("")

                write(ALIGN_CENTER)
                writeLine("Gracias por su pago!")
                writeLine("Conserve este recibo")
                write(FEED_LINES)

                write(CUT_PAPER)
                flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error printing payment ticket", e)
            false
        }
    }

    private fun OutputStream.writeLine(text: String) {
        write(text.toByteArray(Charsets.UTF_8))
        write(LF.toInt())
    }
}

data class TicketItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)
