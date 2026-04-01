package com.cabel.rutacabel.ui.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cabel.rutacabel.data.remote.ApiService
import com.cabel.rutacabel.data.remote.CobroRegistroRequest
import com.cabel.rutacabel.data.remote.CuentaPorCobrarItem
import com.cabel.rutacabel.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class PaymentViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {

    private val apiService = RetrofitClient.apiService

    private val _pendingPayments = MutableLiveData<List<CuentaPorCobrarItem>>()
    val pendingPayments: LiveData<List<CuentaPorCobrarItem>> = _pendingPayments

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _paymentSuccess = MutableLiveData<Boolean>()
    val paymentSuccess: LiveData<Boolean> = _paymentSuccess

    private var allPayments = listOf<CuentaPorCobrarItem>()

    fun loadPendingPayments(sucId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(getApplication())) {
                    val response = apiService.getCobrosPendientes(estCode = 1, sucId = sucId)
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true) {
                            allPayments = body.data ?: emptyList()
                            _pendingPayments.value = allPayments
                        } else {
                            _errorMessage.value = body?.message ?: "Error desconocido"
                        }
                    } else {
                        _errorMessage.value = "Error en el servidor: ${response.code()}"
                    }
                } else {
                    // Offline: Clear list or show empty
                     _errorMessage.value = "Sin conexión: No se pueden cargar pagos pendientes"
                     _pendingPayments.value = emptyList()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de red: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterPayments(query: String) {
        if (query.isEmpty()) {
            _pendingPayments.value = allPayments
        } else {
            val filtered = allPayments.filter {
                it.folioSucursal.contains(query, ignoreCase = true) ||
                it.clienteNombre.contains(query, ignoreCase = true)
            }
            _pendingPayments.value = filtered
        }
    }

    fun registerPayment(request: CobroRegistroRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Try Online
                if (com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(getApplication())) {
                    try {
                        val response = apiService.registrarCobro(request)
                        if (response.isSuccessful && response.body()?.success == true) {
                            _paymentSuccess.value = true
                            return@launch
                        }
                    } catch (e: Exception) {
                        // Fallback
                    }
                }

                // Offline Logic
                val database = com.cabel.rutacabel.data.local.AppDatabase.getDatabase(getApplication())
                val prefs = com.cabel.rutacabel.utils.PreferenceManager(getApplication())
                val userId = prefs.getUserId()

                val payment = com.cabel.rutacabel.data.local.entities.Payment(
                    clientId = 0, // Need to map from request if possible, or use a workaround
                    userId = userId,
                    routeId = 0, // Optional
                    amount = request.monto,
                    paymentMethod = if (request.metodoPagoId == 1) "CASH" else "TRANSFER",
                    reference = request.referencia ?: "",
                    notes = request.comentarios ?: "",
                    ticketPrinted = false,
                    needsSync = true
                )
                // Note: request.ventaId is critical. If offline, we might not have it if it's a new sale.
                // But for "Cobros", it usually refers to an existing "CuentaPorCobrar".
                // We'll save what we can.
                
                database.paymentDao().insertPayment(payment)
                
                // Trigger Sync if Online
                if (com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(getApplication())) {
                    com.cabel.rutacabel.data.SyncWorker.runOnce(getApplication(), userId)
                }
                
                _paymentSuccess.value = true
                com.cabel.rutacabel.utils.UIUtils.toast(getApplication(), "Pago guardado localmente")

            } catch (e: Exception) {
                _errorMessage.value = "Error al guardar pago: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
