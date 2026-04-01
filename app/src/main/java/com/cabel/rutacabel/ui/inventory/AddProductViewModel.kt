package com.cabel.rutacabel.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cabel.rutacabel.data.remote.CatalogItem
import com.cabel.rutacabel.data.remote.ProductoRegistroRequest
import com.cabel.rutacabel.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class AddProductViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.apiService

    private val _categories = MutableLiveData<List<CatalogItem>>()
    val categories: LiveData<List<CatalogItem>> = _categories

    private val _presentations = MutableLiveData<List<CatalogItem>>()
    val presentations: LiveData<List<CatalogItem>> = _presentations

    private val _units = MutableLiveData<List<CatalogItem>>()
    val units: LiveData<List<CatalogItem>> = _units

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    fun loadCatalogs() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val database = com.cabel.rutacabel.data.local.AppDatabase.getDatabase(getApplication())
                val catalogDao = database.catalogDao()

                // Load Categories from Local DB
                val localCategories = catalogDao.getCatalogsByTypeSync("CATEGORY")
                _categories.value = localCategories.map { 
                    CatalogItem(
                        categoriaId = it.remoteId, 
                        nombre = it.name, 
                        metodoPagoId = null, 
                        presentacionId = null, 
                        unidadMedidaId = null, 
                        umSat = null
                    ) 
                }

                // Load Presentations from Local DB
                val localPresentations = catalogDao.getCatalogsByTypeSync("PRESENTATION")
                _presentations.value = localPresentations.map {
                     CatalogItem(
                        categoriaId = null, 
                        nombre = it.name, 
                        metodoPagoId = null, 
                        presentacionId = it.remoteId, 
                        unidadMedidaId = null, 
                        umSat = null
                    )
                }

                // Load Units from Local DB
                val localUnits = catalogDao.getCatalogsByTypeSync("UNIT")
                _units.value = localUnits.map {
                    CatalogItem(
                        categoriaId = null, 
                        nombre = it.name, 
                        metodoPagoId = null, 
                        presentacionId = null, 
                        unidadMedidaId = it.remoteId, 
                        umSat = it.value
                    )
                }
                
                // Trigger Sync if empty (Optional, or rely on SyncWorker)
                if (localCategories.isEmpty() || localUnits.isEmpty()) {
                    // We could trigger a OneTimeSync here if online
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar catálogos locales: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveProduct(
        name: String,
        code: String,
        description: String,
        categoryId: Int,
        presentationId: Int,
        unitId: Int,
        price: Double,
        stock: Int,
        usuarioId: Long,
        sucursalId: Int
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val productRequest = ProductoRegistroRequest(
                nombre = name,
                codigo = code,
                clave = code,
                codigoSAT = "",
                descripcion = description,
                categoriaId = categoryId,
                presentacion = presentationId,
                unidadMedida = unitId,
                cantidad = stock,
                stockMinimo = 1,
                stockMaximo = 100,
                cotizacion = 0,
                foto = null,
                perecedero = false,
                usuarioId = usuarioId,
                sucursalId = sucursalId
            )

            try {
                if (com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(getApplication())) {
                    val response = apiService.registrarProducto(productRequest)
                    if (response.isSuccessful && response.body()?.success == true) {
                        _saveSuccess.value = true
                        // Optionally update local DB with returned ID if needed immediately
                        return@launch
                    }
                }
                
                // Offline or API Failure -> Save Locally
                val database = com.cabel.rutacabel.data.local.AppDatabase.getDatabase(getApplication())
                val product = com.cabel.rutacabel.data.local.entities.Product(
                    code = code,
                    name = name,
                    description = description,
                    category = _categories.value?.find { it.categoriaId == categoryId }?.nombre ?: "",
                    categoryId = categoryId,
                    presentationId = presentationId,
                    unitId = unitId,
                    basePrice = price,
                    stock = stock,
                    minStock = 1,
                    maxStock = 100,
                    unit = _units.value?.find { it.unidadMedidaId == unitId }?.nombre ?: "PZA",
                    sucursalId = sucursalId,
                    codeSat = "",
                    isActive = true,
                    needsSync = true,
                    remoteId = 0 // Indicates it's local only
                )
                database.productDao().insertProduct(product)
                _saveSuccess.value = true
                _errorMessage.value = "Guardado localmente (sin conexión)"

            } catch (e: Exception) {
                 // Final fallback for any other error
                try {
                     val database = com.cabel.rutacabel.data.local.AppDatabase.getDatabase(getApplication())
                     val product = com.cabel.rutacabel.data.local.entities.Product(
                        code = code,
                        name = name,
                        description = description,
                        category = _categories.value?.find { it.categoriaId == categoryId }?.nombre ?: "",
                        categoryId = categoryId,
                        presentationId = presentationId,
                        unitId = unitId,
                        basePrice = price,
                        stock = stock,
                        minStock = 1,
                        maxStock = 100,
                        unit = _units.value?.find { it.unidadMedidaId == unitId }?.nombre ?: "PZA",
                        sucursalId = sucursalId,
                        codeSat = "",
                        isActive = true,
                        needsSync = true,
                        remoteId = 0
                    )
                    database.productDao().insertProduct(product)
                    _saveSuccess.value = true
                    _errorMessage.value = "Guardado localmente (Error: ${e.message})"
                } catch (dbEx: Exception) {
                    _errorMessage.value = "Error al guardar: ${dbEx.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
