package com.cabel.rutacabel.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cabel.rutacabel.data.SyncWorker
import com.cabel.rutacabel.data.local.AppDatabase
import com.cabel.rutacabel.utils.PreferenceManager
import kotlinx.coroutines.launch

data class SyncItemStatus(
    val name: String,
    val pendingCount: Int,
    val totalCount: Int
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val preferenceManager = PreferenceManager(application)

    private val _message = MutableLiveData("")
    val message: LiveData<String> = _message

    // --- Sync Status ---
    private val _syncItems = MutableLiveData<List<SyncItemStatus>>()
    val syncItems: LiveData<List<SyncItemStatus>> = _syncItems

    private val _hasPendingSync = MutableLiveData(false)
    val hasPendingSync: LiveData<Boolean> = _hasPendingSync

    private val _isSyncing = MutableLiveData(false)
    val isSyncing: LiveData<Boolean> = _isSyncing

    private val _syncProgress = MutableLiveData(0)
    val syncProgress: LiveData<Int> = _syncProgress

    private val _syncDetail = MutableLiveData("")
    val syncDetail: LiveData<String> = _syncDetail

    // --- Printer Status ---
    private val _printerName = MutableLiveData<String>()
    val printerName: LiveData<String> = _printerName

    private val _isPrinterConfigured = MutableLiveData<Boolean>()
    val isPrinterConfigured: LiveData<Boolean> = _isPrinterConfigured

    fun loadDashboardData() {
        loadSyncStatus()
        loadPrinterStatus()
    }

    fun loadPrinterStatus() {
        val name = preferenceManager.getBluetoothPrinterName()
        val address = preferenceManager.getBluetoothPrinterAddress()
        
        if (address.isNotEmpty()) {
            _printerName.value = name.ifEmpty { "Impresora Genérica" }
            _isPrinterConfigured.value = true
        } else {
            _printerName.value = "Sin Configurar"
            _isPrinterConfigured.value = false
        }
    }

    fun loadSyncStatus() {
        viewModelScope.launch {
            try {
                val clientsPending = database.clientDao().getPendingSyncCount()
                val clientsTotal = database.clientDao().getTotalCount()
                val productsPending = database.productDao().getPendingSyncCount()
                val productsTotal = database.productDao().getTotalCount()
                val salesPending = database.saleDao().getPendingSyncCount()
                val paymentsPending = database.paymentDao().getPendingSyncCount()
                val ordersPending = database.orderDao().getPendingSyncCount()

                val items = listOf(
                    SyncItemStatus("Clientes", clientsPending, clientsTotal),
                    SyncItemStatus("Productos", productsPending, productsTotal),
                    SyncItemStatus("Ventas", salesPending, 0),
                    SyncItemStatus("Cobros", paymentsPending, 0),
                    SyncItemStatus("Pedidos", ordersPending, 0)
                )

                _syncItems.value = items

                val totalPending = clientsPending + productsPending + salesPending + paymentsPending + ordersPending
                _hasPendingSync.value = totalPending > 0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerSync(userId: Long) {
        _isSyncing.value = true
        _syncProgress.value = 0
        _syncDetail.value = "Iniciante..."
        SyncWorker.runOnce(getApplication(), userId)
        _message.value = "Sincronización iniciada"
    }

    fun onSyncStarted() {
        _isSyncing.value = true
    }

    fun updateSyncProgress(progress: Int, detail: String) {
        if (progress > 0) _syncProgress.value = progress
        if (detail.isNotEmpty()) _syncDetail.value = detail
    }

    fun onSyncCompleted() {
        _isSyncing.value = false
        _syncProgress.value = 100
        loadSyncStatus()
    }
}
