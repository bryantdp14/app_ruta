package com.cabel.rutacabel.ui.order

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cabel.rutacabel.R
import com.cabel.rutacabel.data.remote.CatalogItem
import com.cabel.rutacabel.databinding.ActivityOrderBinding
import com.cabel.rutacabel.services.BluetoothPrinterService
import com.cabel.rutacabel.services.TicketData
import com.cabel.rutacabel.services.TicketRenderer
import com.cabel.rutacabel.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class OrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderBinding
    private lateinit var viewModel: OrderViewModel
    private lateinit var orderProductAdapter: OrderProductAdapter
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var printerService: BluetoothPrinterService
    private val ticketRenderer = TicketRenderer()

    private var lastTicketData: TicketData? = null

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        viewModel = ViewModelProvider(this)[OrderViewModel::class.java]
        printerService = BluetoothPrinterService(this)

        setupUI()
        setupRecyclerView()
        observeViewModel()
        
        // Load inventory for order
        val branchId = preferenceManager.getBranchId()
        val clientId = intent.getLongExtra("CLIENT_ID", 0L)
        viewModel.loadInventory(branchId, clientId)
    }

    override fun onDestroy() {
        super.onDestroy()
        printerService.disconnect()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.filterProducts(s.toString())
            }
        })

        binding.rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            val methodId = if (checkedId == binding.rbCash.id) 1 else 2
            viewModel.setPaymentMethod(methodId)
        }

        binding.btnSaveOrder.setOnClickListener {
            saveOrder()
        }
    }

    private fun setupRecyclerView() {
        orderProductAdapter = OrderProductAdapter(
            onQuantityChanged = { product, quantity ->
                viewModel.updateProductQuantity(product, quantity)
            },
            onPriceListClicked = { product ->
                showPriceSelectionDialog(product)
            }
        )

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(this@OrderActivity)
            adapter = orderProductAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.paymentMethods.observe(this) { methods: List<CatalogItem> ->
            binding.rgPaymentMethod.removeAllViews()
            methods.forEach { method: CatalogItem ->
                val radioButton = android.widget.RadioButton(this).apply {
                    id = android.view.View.generateViewId()
                    text = method.nombre
                    tag = method.metodoPagoId
                    setTextColor(getColor(R.color.text_primary))
                }
                binding.rgPaymentMethod.addView(radioButton)
                if (method.metodoPagoId == 1) { // Default to Efectivo if available
                    radioButton.isChecked = true
                    viewModel.setPaymentMethod(1)
                }
            }
        }

        viewModel.availableProducts.observe(this) { products ->
            orderProductAdapter.submitList(products)
        }

        viewModel.orderTotal.observe(this) { total ->
            binding.tvTotal.text = String.format("Total: $%.2f", total)
        }

        viewModel.client.observe(this) { client ->
            // Update credit card at top
            if (client != null && client.credito) {
                val available = client.creditLimit - client.currentBalance
                binding.cvClientInfo.visibility = android.view.View.VISIBLE
                binding.tvClientName.text = client.name
                binding.tvCreditInfo.text = "Límite: $${String.format("%.2f", client.creditLimit)} | Adeudo: $${String.format("%.2f", client.currentBalance)}"
                binding.tvCreditBadge.text = "$${String.format("%.2f", available)}"
            } else {
                binding.cvClientInfo.visibility = android.view.View.GONE
            }
            val total = viewModel.orderTotal.value ?: 0.0
            binding.tvTotal.text = String.format("Total: $%.2f", total)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnSaveOrder.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        // Listen for ticket to print
        viewModel.ticketToPrint.observe(this) { ticketData ->
            if (ticketData != null) {
                lastTicketData = ticketData
                printTicket(ticketData)
            }
        }

        viewModel.orderSuccess.observe(this) { success ->
            if (success) {
                showOrderSuccessDialog()
            }
        }
    }

    // ──────── Printing ────────

    private fun printTicket(ticketData: TicketData) {
        if (!checkBluetoothPermission()) return

        val printerAddress = preferenceManager.getBluetoothPrinterAddress()
        if (printerAddress.isEmpty()) {
            showNoPrinterDialog(ticketData)
            return
        }

        lifecycleScope.launch {
            val connected = printerService.connectToPrinter(printerAddress)
            if (connected) {
                val printed = printerService.printOrderTicket(ticketData)
                if (printed) {
                    Toast.makeText(this@OrderActivity, "✅ Ticket impreso", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@OrderActivity, "❌ Error al imprimir", Toast.LENGTH_SHORT).show()
                    showPreviewAndShareOption(ticketData)
                }
                printerService.disconnect()
            } else {
                Toast.makeText(this@OrderActivity, "No se pudo conectar a la impresora", Toast.LENGTH_LONG).show()
                // Clear saved printer and show picker
                preferenceManager.setBluetoothPrinterAddress("")
                showNoPrinterDialog(ticketData)
            }
        }
    }

    private fun showNoPrinterDialog(ticketData: TicketData) {
        AlertDialog.Builder(this)
            .setTitle("Impresora no encontrada")
            .setMessage("¿Deseas configurar una impresora o compartir el ticket por WhatsApp?")
            .setPositiveButton("Configurar") { _, _ -> showPrinterPickerDialog(ticketData) }
            .setNeutralButton("Compartir") { _, _ -> showPreviewAndShareOption(ticketData) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun showPrinterPickerDialog(ticketData: TicketData? = null) {
        if (!checkBluetoothPermission()) return

        val devices = printerService.getPairedPrinters()
        if (devices.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sin Impresora")
                .setMessage("No hay dispositivos Bluetooth emparejados.\n\nVe a Ajustes > Bluetooth y empareja tu impresora LOSTRAIN primero.")
                .setPositiveButton("Compartir por WhatsApp") { _, _ -> ticketData?.let { showPreviewAndShareOption(it) } }
                .setNegativeButton("Entendido", null)
                .show()
            return
        }

        val deviceNames = devices.map { "${it.name ?: "Desconocido"}\n${it.address}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("🖨️ Seleccionar Impresora")
            .setItems(deviceNames) { _, which ->
                val device: BluetoothDevice = devices[which]
                preferenceManager.setBluetoothPrinterAddress(device.address)
                preferenceManager.setBluetoothPrinterName(device.name ?: "Impresora")
                Toast.makeText(this, "Impresora: ${device.name}", Toast.LENGTH_SHORT).show()
                // Auto-print after selection
                if (ticketData != null) {
                    printTicket(ticketData)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showOrderSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("✅ Pedido Registrado")
            .setMessage("El pedido se ha guardado correctamente.")
            .setPositiveButton("Aceptar") { _, _ -> finish() }
            .setNeutralButton("🖨️ Reimprimir") { _, _ ->
                lastTicketData?.let { printTicket(it) }
                // Don't finish — let user reprint or accept
                showOrderSuccessDialog()
            }
            .setNegativeButton("Compartir") { _, _ ->
                lastTicketData?.let { showPreviewAndShareOption(it) }
                showOrderSuccessDialog()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPreviewAndShareOption(ticketData: TicketData) {
        val bitmap = ticketRenderer.renderTicket(ticketData)
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        imageView.setPadding(20, 20, 20, 20)
        imageView.adjustViewBounds = true

        AlertDialog.Builder(this)
            .setTitle("Vista Previa del Ticket")
            .setView(imageView)
            .setPositiveButton("Compartir WhatsApp") { _, _ -> shareTicketImage(bitmap, ticketData.getFolio()) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun shareTicketImage(bitmap: Bitmap, folio: String) {
        lifecycleScope.launch {
            val uri = saveBitmapToCache(bitmap, "ticket_$folio.png")
            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Ticket de Pedido $folio - Distribuidora CABEL")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Enviar ticket por..."))
            } else {
                Toast.makeText(this@OrderActivity, "Error al generar imagen de ticket", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveBitmapToCache(bitmap: Bitmap, filename: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, filename)
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            FileProvider.getUriForFile(this@OrderActivity, "${packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun checkBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_PERMISSION
                )
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Retry printing with last ticket
                lastTicketData?.let { printTicket(it) }
            } else {
                Toast.makeText(this, "Permiso Bluetooth requerido para imprimir", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ──────── Dialogs ────────

    private fun showPriceSelectionDialog(product: OrderProduct) {
        if (product.prices.isEmpty()) {
            Toast.makeText(this, "No hay listas de precios disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val priceNames = product.prices.map { 
            "${it.listaPrecio}: $%.2f".format(it.precio)
        }.toTypedArray()

        var selectedIndex = product.prices.indexOfFirst { it.idLista == product.selectedPrice?.idLista }
        if (selectedIndex == -1) selectedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Lista de Precio")
            .setSingleChoiceItems(priceNames, selectedIndex) { dialog, which ->
                val selectedPrice = product.prices[which]
                viewModel.updateProductPrice(product, selectedPrice)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveOrder() {
        val clientName = intent.getStringExtra("CLIENT_NAME") ?: "Cliente"
        val clientTel = intent.getStringExtra("CLIENT_TEL") ?: ""
        val branchId = preferenceManager.getBranchId()

        viewModel.saveOrder(branchId, clientName, clientTel)
    }
}
