package com.cabel.rutacabel.ui.client

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cabel.rutacabel.data.remote.ClienteItem
import com.cabel.rutacabel.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class ClientViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.apiService

    private val _clients = MutableLiveData<List<ClienteItem>>()
    val clients: LiveData<List<ClienteItem>> = _clients

    private val _filteredClients = MutableLiveData<List<ClienteItem>>()
    val filteredClients: LiveData<List<ClienteItem>> = _filteredClients

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private var currentFilter: ClientFilter = ClientFilter.ALL

    enum class ClientFilter {
        ALL, PROSPECTS, WITH_CREDIT, TO_VISIT
    }

    fun loadClients(sucursalId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val database = com.cabel.rutacabel.data.local.AppDatabase.getDatabase(getApplication())
                val localClients = database.clientDao().getAllClientsList()
                
                if (localClients.isNotEmpty()) {
                    val clientList = localClients.map { c ->
                        ClienteItem(
                            clienteId = c.remoteId ?: 0,
                            nombre = c.name,
                            direccion = c.address,
                            colonia = c.colonia,
                            municipio = c.municipio,
                            estado = "",
                            telefono = c.phone,
                            email = c.email,
                            rfc = c.taxId,
                            regimen = "",
                            esProspecto = if (c.esProspecto) 1 else 0,
                            credito = if (c.credito) 1 else 0,
                            limiteCredito = c.creditLimit,
                            latitude = c.latitude,
                            longitude = c.longitude,
                            ultimaVisita = null,
                            adeudo = 0,
                            montoAdeudo = c.currentBalance
                        )
                    }
                    _clients.value = clientList
                    applyFilter(currentFilter)
                }

                if (com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(getApplication())) {
                    val response = apiService.getClientes(sucursalId)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val clientList = response.body()?.data ?: emptyList()
                        _clients.value = clientList
                        applyFilter(currentFilter)
                    }
                } else if (localClients.isEmpty()) {
                    _errorMessage.value = "Sin conexión y no hay clientes guardados."
                }
            } catch (e: Exception) {
                if (_clients.value.isNullOrEmpty()) {
                    _errorMessage.value = "Error de conexión: ${e.message}"
                } else {
                    _errorMessage.value = "Mostrando datos locales. Error de red: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchClients(query: String) {
        val allClients = _clients.value ?: return
        if (query.isEmpty()) {
            applyFilter(currentFilter)
        } else {
            _filteredClients.value = allClients.filter { client ->
                client.nombre.contains(query, ignoreCase = true) ||
                client.direccion.contains(query, ignoreCase = true) ||
                client.telefono?.contains(query, ignoreCase = true) == true
            }
        }
    }

    fun setFilter(filter: ClientFilter) {
        currentFilter = filter
        applyFilter(filter)
    }

    private fun applyFilter(filter: ClientFilter) {
        val allClients = _clients.value ?: return
        _filteredClients.value = when (filter) {
            ClientFilter.ALL -> allClients
            ClientFilter.PROSPECTS -> allClients.filter { it.esProspecto == 1 }
            ClientFilter.WITH_CREDIT -> allClients.filter { it.credito == 1 }
            ClientFilter.TO_VISIT -> allClients.filter { it.ultimaVisita == null }
        }
    }
}
