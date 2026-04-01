package com.cabel.rutacabel.ui.route

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cabel.rutacabel.RutaCABELApplication
import com.cabel.rutacabel.data.local.AppDatabase
import com.cabel.rutacabel.data.local.entities.Client
import com.cabel.rutacabel.data.local.entities.RouteDetail
import com.cabel.rutacabel.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val clientDao = database.clientDao()
    private val routeDetailDao = database.routeDetailDao()
    private val apiService = RetrofitClient.apiService

    val clients: LiveData<List<Client>> = clientDao.getAllClients()
    
    private val _routeDetails = MutableLiveData<List<RouteDetail>>()
    val routeDetails: LiveData<List<RouteDetail>> = _routeDetails

    private val _searchResults = MutableLiveData<List<Client>>()
    val searchResults: LiveData<List<Client>> = _searchResults
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadRouteDetails(empleadoId: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = apiService.getRutasDetalles(empleadoId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val routeItems = response.body()?.data ?: emptyList()
                    
                    // Convert API response to local entities
                    val routeDetails = routeItems.map { item ->
                        RouteDetail(
                            rutaId = item.rutaId,
                            nombreRuta = item.nombreRuta,
                            consecutivo = item.consecutivo,
                            clienteId = 0, // Will be mapped if needed
                            clienteNombre = item.clienteNombre,
                            folioSucursal = item.folioSucursal,
                            latitud = item.latitud,
                            longitud = item.longitud,
                            estatus = item.estatus
                        )
                    }
                    
                    // Save to local database
                    routeDetailDao.insertAll(routeDetails)
                    
                    // Update LiveData
                    _routeDetails.value = routeDetails
                } else {
                    _errorMessage.value = response.body()?.message ?: "Error al cargar rutas"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
                // Load from local database as fallback
                loadLocalRouteDetails()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadLocalRouteDetails() {
        val localRoutes = routeDetailDao.getUnsyncedRouteDetails()
        _routeDetails.postValue(localRoutes)
    }

    fun searchClients(query: String) {
        if (query.isEmpty()) {
            return
        }
        viewModelScope.launch {
            _searchResults.value = clientDao.searchClients(query)
        }
    }

    fun addClient(client: Client) {
        viewModelScope.launch {
            clientDao.insertClient(client)
        }
    }
    
    fun markClientAsVisited(routeDetailId: Long) {
        viewModelScope.launch {
            routeDetailDao.markAsVisited(
                id = routeDetailId,
                visitado = true,
                fechaVisita = System.currentTimeMillis()
            )
            routeDetailDao.markForSync(routeDetailId)
        }
    }

    private val routeDao = database.routeDao()
    private val _routeState = MutableLiveData<com.cabel.rutacabel.data.local.entities.Route?>()
    val routeState: LiveData<com.cabel.rutacabel.data.local.entities.Route?> = _routeState

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun checkRouteStatus(userId: Long) {
        viewModelScope.launch {
            _routeState.value = routeDao.getActiveRoute(userId)
        }
    }

    fun startRoute(userId: Long) {
        viewModelScope.launch {
            try {
                val existingRoute = routeDao.getActiveRoute(userId)
                if (existingRoute != null) {
                    _message.value = "Ya existe una ruta activa"
                    return@launch
                }

                val newRoute = com.cabel.rutacabel.data.local.entities.Route(
                    userId = userId,
                    routeName = "Ruta ${System.currentTimeMillis()}",
                    startDate = System.currentTimeMillis(),
                    status = "ACTIVE"
                )
                val routeId = routeDao.insertRoute(newRoute)
                _routeState.value = newRoute.copy(id = routeId)
                _message.value = "Ruta iniciada"

                com.cabel.rutacabel.data.SyncWorker.scheduleSync(getApplication(), userId)
            } catch (e: Exception) {
                _message.value = "Error al iniciar ruta"
            }
        }
    }

    fun endRoute() {
        viewModelScope.launch {
            try {
                val currentRoute = _routeState.value
                if (currentRoute != null) {
                    val updatedRoute = currentRoute.copy(
                        endDate = System.currentTimeMillis(),
                        status = "COMPLETED"
                    )
                    routeDao.updateRoute(updatedRoute)
                    _routeState.value = null
                    _message.value = "Ruta finalizada"
                }
            } catch (e: Exception) {
                _message.value = "Error al finalizar ruta"
            }
        }
    }
}
