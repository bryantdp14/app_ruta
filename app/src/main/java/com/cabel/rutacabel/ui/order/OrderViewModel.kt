package com.cabel.rutacabel.ui.order

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cabel.rutacabel.data.remote.CatalogItem
import com.cabel.rutacabel.data.remote.InventarioItem
import com.cabel.rutacabel.data.remote.OrderItemRequest
import com.cabel.rutacabel.data.remote.OrderRegisterRequest
import com.cabel.rutacabel.data.remote.OrderResponse
import com.cabel.rutacabel.data.remote.RetrofitClient
import com.cabel.rutacabel.services.TicketData
import com.cabel.rutacabel.services.TicketItem
import kotlinx.coroutines.launch

data class OrderProduct(
    val id: Long, // Local DB ID
    val inventarioItem: InventarioItem,
    var quantity: Int = 0,
    var prices: List<com.cabel.rutacabel.data.remote.PrecioItem> = emptyList(),
    var selectedPrice: com.cabel.rutacabel.data.remote.PrecioItem? = null
)

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.apiService

    private val _availableProducts = MutableLiveData<List<OrderProduct>>()
    val availableProducts: LiveData<List<OrderProduct>> = _availableProducts

    private val _orderTotal = MutableLiveData<Double>(0.0)
    val orderTotal: LiveData<Double> = _orderTotal

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _orderSuccess = MutableLiveData<Boolean>()
    val orderSuccess: LiveData<Boolean> = _orderSuccess

    private val _ticketToPrint = MutableLiveData<TicketData?>()
    val ticketToPrint: LiveData<TicketData?> = _ticketToPrint

    private val _paymentMethodId = MutableLiveData<Int>(1) // Default 1: Efectivo
    val paymentMethodId: LiveData<Int> = _paymentMethodId

    private val _paymentMethods = MutableLiveData<List<CatalogItem>>()
    val paymentMethods: LiveData<List<CatalogItem>> = _paymentMethods

    private val _client = MutableLiveData<com.cabel.rutacabel.data.local.entities.Client?>()
    val client: LiveData<com.cabel.rutacabel.data.local.entities.Client?> = _client

    // Cache of user selections (quantity & price) keyed by local Product.id
    private val selectionCache = mutableMapOf<Long, Pair<Int, com.cabel.rutacabel.data.remote.PrecioItem?>>()
    private var currentQuery = ""
    private var currentSucursalId = 0
    private val PAGE_SIZE = 50

    fun loadInventory(sucursalId: Int, clientId: Long = 0) {
        currentSucursalId = sucursalId
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val database = com.cabel.rutacabel.data.local.AppDatabase.getDatabase(getApplication())
                
                // 1. Load Client if provided
                if (clientId > 0) {
                    _client.value = database.clientDao().getClientByRemoteId(clientId)
                }

                // 2. Check if local prices exist. If not, fetch from API and save
                val samplePrices = database.productPriceDao().getPricesByProductIds(
                    database.productDao().searchProductsPaged("", 1, 0).mapNotNull { it.remoteId }
                )
                if (samplePrices.isEmpty()) {
                    fetchAndSavePricesFromApi(database)
                }

                // 3. Load first page of products
                loadProductsPage(database, "", 0)

            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar datos locales: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchAndSavePricesFromApi(database: com.cabel.rutacabel.data.local.AppDatabase) {
        try {
            if (!com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(getApplication())) return

            val response = apiService.getPrecios(0) // 0 = all products
            if (response.isSuccessful && response.body()?.success == true) {
                val prices = response.body()?.data ?: emptyList()
                val now = System.currentTimeMillis()

                // Group by product and save
                val byProduct = prices.groupBy { it.productoId }
                for ((productId, productPrices) in byProduct) {
                    val localPrices = productPrices.map { p ->
                        com.cabel.rutacabel.data.local.entities.ProductPrice(
                            remotePrecioId = p.precioId,
                            productoId = p.productoId,
                            idLista = p.idLista,
                            listaPrecio = p.listaPrecio,
                            precio = p.precio,
                            cantMin = p.cantMin,
                            cantMax = p.cantMax,
                            publico = (p.publico ?: 0) == 1,
                            lastSync = now
                        )
                    }
                    // Only save if product exists locally (FK constraint)
                    val productExists = database.productDao().getProductByRemoteId(productId) != null
                    if (productExists) {
                        database.productPriceDao().replacePrices(productId, localPrices)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Non-fatal: fallback to basePrice will be used
        }
    }

    private suspend fun loadProductsPage(
        database: com.cabel.rutacabel.data.local.AppDatabase,
        query: String,
        offset: Int
    ) {
        val products = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val localProducts = database.productDao().searchProductsPaged(query, PAGE_SIZE, offset)
            if (localProducts.isEmpty()) return@withContext emptyList<OrderProduct>()

            // Batch-fetch prices only for this page's products
            val productIds = localProducts.mapNotNull { it.remoteId }
            val allPrices = if (productIds.isNotEmpty()) {
                database.productPriceDao().getPricesByProductIds(productIds)
            } else emptyList()
            val pricesByProduct = allPrices.groupBy { it.productoId }

            localProducts.map { p ->
                val localPrices = pricesByProduct[p.remoteId] ?: emptyList()
                
                var apiPrices = localPrices.map { lp ->
                    com.cabel.rutacabel.data.remote.PrecioItem(
                        productoId = lp.productoId,
                        producto = null,
                        categoria = null,
                        idLista = lp.idLista,
                        listaPrecio = lp.listaPrecio,
                        precioId = lp.remotePrecioId,
                        precio = lp.precio,
                        cantMin = lp.cantMin,
                        cantMax = lp.cantMax,
                        publico = if (lp.publico) 1 else 0,
                        fechaMod = null
                    )
                }

                // Fallback: if no price lists exist, use the product's basePrice
                if (apiPrices.isEmpty() && p.basePrice > 0) {
                    apiPrices = listOf(
                        com.cabel.rutacabel.data.remote.PrecioItem(
                            productoId = p.remoteId ?: 0,
                            producto = null,
                            categoria = null,
                            idLista = 0,
                            listaPrecio = "Precio Base",
                            precioId = 0,
                            precio = p.basePrice,
                            cantMin = null,
                            cantMax = null,
                            publico = 1,
                            fechaMod = null
                        )
                    )
                }

                val defaultPriceItem = apiPrices.find { it.publico == 1 } ?: apiPrices.firstOrNull()

                // Restore user selections from cache
                val cached = selectionCache[p.id]
                val qty = cached?.first ?: 0
                val selectedPrice = cached?.second ?: defaultPriceItem

                OrderProduct(
                    id = p.id,
                    inventarioItem = InventarioItem(
                        productoId = p.remoteId ?: 0,
                        codigo = p.code,
                        nombre = p.name,
                        presentacion = p.description,
                        almacen = p.category,
                        sucursal = "",
                        cantidad = p.stock,
                        ultimaCompra = null,
                        ultimoCosto = p.basePrice
                    ),
                    quantity = qty,
                    prices = apiPrices,
                    selectedPrice = selectedPrice
                )
            }
        }

        _availableProducts.value = products
    }

    fun filterProducts(query: String) {
        if (query == currentQuery) return
        currentQuery = query
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val database = com.cabel.rutacabel.data.local.AppDatabase.getDatabase(getApplication())
                loadProductsPage(database, query, 0)
            } catch (e: Exception) {
                _errorMessage.value = "Error al buscar productos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPaymentMethods() {
        viewModelScope.launch {
            try {
                 val database = com.cabel.rutacabel.data.local.AppDatabase.getDatabase(getApplication())
                 val catalogDao = database.catalogDao()
                 
                 // Try Local First
                 val localPMs = catalogDao.getCatalogsByTypeSync("PAYMENT_METHOD")
                 
                 if (localPMs.isNotEmpty()) {
                     _paymentMethods.value = localPMs.map { 
                         CatalogItem(
                             categoriaId = null,
                             metodoPagoId = it.remoteId, 
                             nombre = it.name,
                             presentacionId = null,
                             unidadMedidaId = null,
                             umSat = null
                         )
                     }
                 } else {
                     // Fallback to Online if empty
                      if (com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(getApplication())) {
                        val response = apiService.getPaymentMethods()
                        if (response.isSuccessful && response.body()?.success == true) {
                            _paymentMethods.value = response.body()?.data ?: emptyList()
                        }
                      }
                 }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun setPaymentMethod(id: Int) {
        _paymentMethodId.value = id
    }

    fun updateProductQuantity(product: OrderProduct, quantity: Int) {
        product.quantity = quantity
        // Cache the selection by local ID
        selectionCache[product.id] = Pair(quantity, product.selectedPrice)
        calculateTotal()
    }

    fun updateProductPrice(product: OrderProduct, price: com.cabel.rutacabel.data.remote.PrecioItem) {
        product.selectedPrice = price
        // Cache the selection by local ID
        selectionCache[product.id] = Pair(product.quantity, price)
        calculateTotal()
        // Refresh UI
        _availableProducts.value = _availableProducts.value
    }

    private fun calculateTotal() {
        // Sum from selectionCache (all user selections across pages)
        var total = 0.0
        for ((productId, selection) in selectionCache) {
            val qty = selection.first
            val price = selection.second?.precio ?: 0.0
            total += price * qty
        }
        _orderTotal.value = total
    }

    fun saveOrder(branchId: Int, clientName: String, clientTel: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Collect selected products from cache (keyed by local Product.id)
                val selectedEntries = selectionCache.filter { it.value.first > 0 }
                if (selectedEntries.isEmpty()) {
                    _errorMessage.value = "Debe seleccionar al menos un producto"
                    return@launch
                }

                val total = _orderTotal.value ?: 0.0
                val methodId = _paymentMethodId.value ?: 1
                val currentClient = _client.value

                // 1. Business Rules Validation
                if (methodId == 2) { // Crédito
                    if (currentClient == null || !currentClient.credito) {
                        _errorMessage.value = "El cliente no cuenta con crédito autorizado"
                        return@launch
                    }
                    val newBalance = currentClient.currentBalance + total
                    if (newBalance > currentClient.creditLimit) {
                        _errorMessage.value = "Límite de crédito excedido. Disponible: $%.2f"
                            .format(currentClient.creditLimit - currentClient.currentBalance)
                        return@launch
                    }
                }

                // 2. Save to Local DB
                val database = com.cabel.rutacabel.data.local.AppDatabase.getDatabase(getApplication())
                val prefs = com.cabel.rutacabel.utils.PreferenceManager(getApplication())
                val userId = prefs.getUserId()
                val clientId = currentClient?.remoteId ?: 0L

                val order = com.cabel.rutacabel.data.local.entities.Order(
                    clientId = clientId,
                    userId = userId,
                    branchId = branchId,
                    total = total,
                    paymentMethodId = methodId,
                    clientName = clientName,
                    clientPhone = clientTel,
                    needsSync = true,
                    status = "PENDING",
                    orderDate = System.currentTimeMillis()
                )

                val orderId = database.orderDao().insertOrder(order)

                val details = selectedEntries.map { (localProductId, selection) ->
                    val qty = selection.first
                    val unitPrice = selection.second?.precio ?: 0.0
                    
                    // Get remote ID for the order detail record
                    val p = database.productDao().getProductById(localProductId)
                    val remoteId = p?.remoteId ?: 0L
                    
                    com.cabel.rutacabel.data.local.entities.OrderDetail(
                        orderId = orderId,
                        productId = remoteId,
                        quantity = qty,
                        unitPrice = unitPrice,
                        total = unitPrice * qty
                    )
                }
                database.orderDetailDao().insertDetails(details)

                // 3. Update Client Status (Prospect to Client)
                if (currentClient != null && currentClient.esProspecto) {
                    val updatedClient = currentClient.copy(
                        esProspecto = false,
                        needsSync = true
                    )
                    database.clientDao().updateClient(updatedClient)
                }

                // 4. Trigger Sync if Online
                if (com.cabel.rutacabel.utils.NetworkUtils.isNetworkAvailable(getApplication())) {
                    com.cabel.rutacabel.data.SyncWorker.runOnce(getApplication(), userId)
                    com.cabel.rutacabel.utils.UIUtils.toast(getApplication(), "Pedido guardado. Sincronizando...")
                } else {
                    com.cabel.rutacabel.utils.UIUtils.toast(getApplication(), "Pedido guardado localmente (sin conexión)")
                }

                // 5. Build ticket for printing
                val ticketItems = selectedEntries.map { (localProductId, selection) ->
                    val qty = selection.first
                    val price = selection.second
                    // Find product from memory or DB
                    val product = _availableProducts.value?.find { 
                        it.id == localProductId 
                    }
                    TicketItem(
                        quantity = qty,
                        code = product?.inventarioItem?.codigo ?: localProductId.toString(),
                        description = product?.inventarioItem?.nombre ?: "Producto",
                        unitPrice = price?.precio ?: 0.0,
                        total = (price?.precio ?: 0.0) * qty
                    )
                }

                val paymentMethodName = if (methodId == 1) "Efectivo" else "Crédito"
                val ticketData = TicketData(
                    orderId = orderId,
                    orderDate = System.currentTimeMillis(),
                    clientName = clientName,
                    sellerName = prefs.getFullName(),
                    items = ticketItems,
                    subtotal = total,
                    total = total,
                    paymentMethod = paymentMethodName,
                    isCredit = methodId == 2,
                    creditLimit = currentClient?.creditLimit ?: 0.0,
                    creditAvailable = if (currentClient != null) 
                        currentClient.creditLimit - currentClient.currentBalance - total else 0.0
                )
                _ticketToPrint.postValue(ticketData)

                _orderSuccess.value = true

            } catch (e: Exception) {
                _errorMessage.value = "Error al guardar pedido: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

