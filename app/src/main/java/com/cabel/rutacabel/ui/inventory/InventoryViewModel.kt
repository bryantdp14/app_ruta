package com.cabel.rutacabel.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cabel.rutacabel.data.local.AppDatabase
import com.cabel.rutacabel.data.remote.InventarioItem
import com.cabel.rutacabel.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val productDao = database.productDao()
    private val apiService = RetrofitClient.apiService

    // Internal observation of DB
    private val dbProducts = productDao.getAllProducts()

    private val _inventoryItems = MediatorLiveData<List<InventarioItem>>()
    val inventoryItems: LiveData<List<InventarioItem>> = _inventoryItems
    
    // Expose all items for category chip generation
    val allItems: LiveData<List<InventarioItem>> = _inventoryItems

    private val _filteredItems = MutableLiveData<List<InventarioItem>>()
    val filteredItems: LiveData<List<InventarioItem>> = _filteredItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private var currentSearchQuery: String = ""
    private var currentCategoryFilter: String? = null

    init {
        _inventoryItems.addSource(dbProducts) { products ->
            val items = products.map { p ->
                InventarioItem(
                    productoId = p.remoteId ?: p.id,
                    codigo = p.code,
                    nombre = p.name,
                    almacen = p.category, // Mapped from SyncWorker
                    sucursal = "", // Not critical for List
                    cantidad = p.stock,
                    ultimaCompra = "",
                    presentacion = p.description, // Mapped from SyncWorker
                    ultimoCosto = p.basePrice,
                    precioPublico = p.basePrice
                )
            }
            _inventoryItems.value = items
            applyFilters()
        }
    }

    fun loadInventory(sucursalId: Int) {
        refreshInventory(sucursalId)
    }

    private fun refreshInventory(sucursalId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // We don't block UI here, just update DB if successful
                val response = apiService.getInventario(sucursalId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val items = response.body()?.data ?: emptyList()
                    
                    // Fetch prices
                    var priceMap: Map<Long, Double> = emptyMap()
                    try {
                        val pricesResponse = apiService.getPrecios(0)
                        if (pricesResponse.isSuccessful && pricesResponse.body()?.success == true) {
                            val pricesList = pricesResponse.body()?.data ?: emptyList()
                            priceMap = pricesList.filter { it.publico == 1 }
                                .associateBy({ it.productoId }, { it.precio })
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Update DB
                    val products = items.map { item ->
                        val price = priceMap[item.productoId] ?: item.ultimoCosto
                        com.cabel.rutacabel.data.local.entities.Product(
                            remoteId = item.productoId,
                            code = item.codigo ?: "",
                            name = item.nombre,
                            description = item.presentacion,
                            category = item.almacen,
                            basePrice = price,
                            stock = item.cantidad,
                            unit = "PZA",
                            sucursalId = sucursalId,
                            isActive = true,
                            lastSync = System.currentTimeMillis()
                        )
                    }
                    productDao.insertProducts(products)
                } 
                // If fails, we just don't update DB. UI already shows local data.
            } catch (e: Exception) {
                // Only show error if list is empty? Or just log?
                // _errorMessage.value = "Error de conexión: ${e.message}"
                android.util.Log.e("InventoryViewModel", "Error refreshing inventory", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchInventory(query: String) {
        currentSearchQuery = query
        applyFilters()
    }
    
    fun filterByCategory(category: String?) {
        currentCategoryFilter = category
        applyFilters()
    }
    
    private fun applyFilters() {
        var items = _inventoryItems.value ?: return
        
        // Apply category filter
        if (currentCategoryFilter != null) {
            items = items.filter { it.presentacion == currentCategoryFilter }
        }
        
        // Apply search filter
        if (currentSearchQuery.isNotEmpty()) {
            items = items.filter { item ->
                item.productoId.toString().contains(currentSearchQuery, ignoreCase = true) ||
                item.codigo?.contains(currentSearchQuery, ignoreCase = true) == true ||
                item.nombre.contains(currentSearchQuery, ignoreCase = true) ||
                item.almacen.contains(currentSearchQuery, ignoreCase = true) ||
                item.presentacion.contains(currentSearchQuery, ignoreCase = true)
            }
        }
        
        _filteredItems.value = items
    }
}
