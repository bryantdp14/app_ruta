package com.cabel.rutacabel.data

import android.content.Context
import androidx.work.*
import com.cabel.rutacabel.data.local.AppDatabase
import com.cabel.rutacabel.data.remote.RetrofitClient
import com.cabel.rutacabel.data.remote.SyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = AppDatabase.getDatabase(context)
    private val apiService = RetrofitClient.apiService

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userId = inputData.getLong("USER_ID", 0L)
            if (userId == 0L) return@withContext Result.failure()

            sendSyncLifecycleBroadcast(started = true)
            
            // Progress 0%
            sendProgressBroadcast(0, "Iniciando sincronización...")

            // Step 1: Uploads (0% - 50%)
            sendProgressBroadcast(5, "Enviando clientes nuevos...")
            syncClients(userId)
            
            sendProgressBroadcast(15, "Enviando productos...")
            syncProducts(userId)
            
            sendProgressBroadcast(25, "Enviando ventas...")
            syncSales(userId)
            
            sendProgressBroadcast(35, "Enviando cobros...")
            syncPayments(userId)
            
            sendProgressBroadcast(45, "Enviando pedidos...")
            syncOrders(userId)
            
            // Step 2: Downloads (50% - 100%)
            sendProgressBroadcast(55, "Descargando catálogos...")
            syncCatalogs()
            
            sendProgressBroadcast(70, "Descargando clientes...")
            syncClientsDownload(userId)
            
            sendProgressBroadcast(85, "Descargando inventario...")
            syncInventoryDownload(userId)

            sendProgressBroadcast(100, "Sincronización completada")
            sendSyncLifecycleBroadcast(started = false)

            Result.success()
        } catch (e: java.net.SocketTimeoutException) {
            e.printStackTrace()
            sendBroadcast("Error: Tiempo de espera agotado. Reintentando...", true)
            sendSyncLifecycleBroadcast(started = false)
            Result.retry()
        } catch (e: java.net.ConnectException) {
            e.printStackTrace()
            sendBroadcast("Error: No se pudo conectar al servidor. Reintentando...", true)
            sendSyncLifecycleBroadcast(started = false)
            Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = e.message ?: "Error desconocido"
            sendBroadcast("Error en la sincronización: $errorMessage", true)
            sendSyncLifecycleBroadcast(started = false)
            Result.retry()
        }
    }

    private suspend fun syncClientsDownload(userId: Long) {
        val context = applicationContext
        val prefs = context.getSharedPreferences("RutaCABELPrefs", Context.MODE_PRIVATE)
        val branchId = prefs.getInt("branchId", 0)

        if (branchId == 0) return

        try {
            val response = apiService.getClientes(branchId)
            if (response.isSuccessful && response.body()?.success == true) {
                val items = response.body()?.data ?: emptyList()
                if (items.isEmpty()) return

                val now = System.currentTimeMillis()

                // Build batch list — use insertClients (OnConflictStrategy.REPLACE)
                val clientsToUpsert = items.mapNotNull { item ->
                    if (item.clienteId == 0L) return@mapNotNull null

                    val existing = database.clientDao().getClientByRemoteId(item.clienteId)
                    if (existing != null) {
                        existing.copy(
                            name = item.nombre,
                            phone = item.telefono ?: "",
                            address = item.direccion,
                            colonia = item.colonia ?: "",
                            municipio = item.municipio ?: "",
                            email = item.email ?: "",
                            taxId = item.rfc ?: "",
                            esProspecto = (item.esProspecto ?: 0) == 1,
                            credito = item.credito == 1,
                            creditLimit = item.limiteCredito,
                            currentBalance = item.montoAdeudo ?: 0.0,
                            latitude = item.latitude ?: 0.0,
                            longitude = item.longitude ?: 0.0,
                            isActive = true,
                            lastSync = now
                        )
                    } else {
                        com.cabel.rutacabel.data.local.entities.Client(
                            remoteId = item.clienteId,
                            name = item.nombre,
                            phone = item.telefono ?: "",
                            address = item.direccion,
                            colonia = item.colonia ?: "",
                            municipio = item.municipio ?: "",
                            email = item.email ?: "",
                            taxId = item.rfc ?: "",
                            esProspecto = (item.esProspecto ?: 0) == 1,
                            credito = item.credito == 1,
                            creditLimit = item.limiteCredito,
                            currentBalance = item.montoAdeudo ?: 0.0,
                            latitude = item.latitude ?: 0.0,
                            longitude = item.longitude ?: 0.0,
                            routeOrder = 0,
                            isActive = true,
                            needsSync = false,
                            lastSync = now
                        )
                    }
                }

                // Single batch insert/replace
                if (clientsToUpsert.isNotEmpty()) {
                    database.clientDao().insertClients(clientsToUpsert)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncClients(userId: Long) {
        val clientsToSync = database.clientDao().getClientsToSync()
        if (clientsToSync.isNotEmpty()) {
            val request = SyncRequest(
                data = clientsToSync,
                userId = userId,
                lastSync = System.currentTimeMillis()
            )
            val response = apiService.syncClientsUpload(request)
            if (response.isSuccessful && response.body()?.success == true) {
                clientsToSync.forEach { client ->
                    database.clientDao().updateClient(
                        client.copy(needsSync = false, lastSync = System.currentTimeMillis())
                    )
                }
            }
        }
    }

    private suspend fun syncProducts(userId: Long) {
        val productsToSync = database.productDao().getProductsToSync()
        if (productsToSync.isNotEmpty()) {
            val request = SyncRequest(
                data = productsToSync,
                userId = userId,
                lastSync = System.currentTimeMillis()
            )
            val response = apiService.syncProductsUpload(request)
            if (response.isSuccessful && response.body()?.success == true) {
                productsToSync.forEach { product ->
                    database.productDao().updateProduct(
                        product.copy(needsSync = false, lastSync = System.currentTimeMillis())
                    )
                }
            }
        }
    }

    private suspend fun syncSales(userId: Long) {
        val salesToSync = database.saleDao().getSalesToSync()
        if (salesToSync.isNotEmpty()) {
            salesToSync.forEach { sale ->
                val details = database.saleDetailDao().getDetailsBySale(sale.id)
                val saleRequest = mapOf(
                    "UsuarioID" to userId,
                    "SucursalID" to 1, // Default or dynamic
                    "ClienteID" to sale.remoteId,
                    "TotalVenta" to sale.total,
                    "Detalles" to details.map { dSorted: com.cabel.rutacabel.data.local.entities.SaleDetail ->
                        val product = database.productDao().getProductById(dSorted.productId)
                        mapOf(
                            "ProductoID" to (product?.remoteId ?: dSorted.productId),
                            "Cantidad" to dSorted.quantity,
                            "PrecioUnitario" to dSorted.unitPrice
                        )
                    }
                )
                val response = apiService.createDirectSale(saleRequest)
                if (response.isSuccessful && response.body()?.success == true) {
                    database.saleDao().updateSale(
                        sale.copy(needsSync = false, lastSync = System.currentTimeMillis())
                    )
                }
            }
        }
    }

    private suspend fun syncPayments(userId: Long) {
        val payments = database.paymentDao().getPaymentsToSync()
        if (payments.isNotEmpty()) {
            payments.forEach { payment ->
                
                val request = com.cabel.rutacabel.data.remote.CobroRegistroRequest(
                    folio = "", // Folio generated by server or empty
                    monto = payment.amount,
                    metodoPagoId = if (payment.paymentMethod == "CASH") 1 else 2, // 1: Efectivo, 2: Transferencia (Simplified)
                    referencia = payment.reference,
                    comentarios = payment.notes,
                    sucursalId = 0, // We might need store ID here. Defaulting to 0 for now as it might be implied by user or not needed.
                    usuarioId = userId
                )

                try {
                    val response = apiService.registrarCobro(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        // Mark as synced. Since PaymentDao doesn't have markAsSynced, we update the entity.
                        // We need to add 'needsSync = false' update to PaymentDao or update object manually.
                         val updatedPayment = payment.copy(needsSync = false, remoteId = null, lastSync = System.currentTimeMillis())
                         database.paymentDao().updatePayment(updatedPayment)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SyncWorker", "ERROR syncPayments: ${e.message}", e)
                    sendBroadcast("Error al enviar cobros", true)
                    throw e
                }
            }
        }
    }

    private suspend fun syncIncidents(userId: Long) {
        // Implement if backend has sp_Incidents
    }

    private suspend fun syncOrders(userId: Long) {
        val ordersToSync = database.orderDao().getOrdersToSync()
        if (ordersToSync.isNotEmpty()) {
            ordersToSync.forEach { order ->
                val details = database.orderDetailDao().getDetailsByOrder(order.id)
                
                // Map to API Request
                val items = details.map { d ->
                    val product = database.productDao().getProductById(d.productId)
                    com.cabel.rutacabel.data.remote.OrderItemRequest(
                        producto = product?.remoteId ?: d.productId,
                        cantidad = d.quantity,
                        precio = d.unitPrice
                    )
                }

                val request = com.cabel.rutacabel.data.remote.OrderRegisterRequest(
                    productos = items,
                    sucursal = order.branchId,
                    total = order.total,
                    nombre = order.clientName,
                    tel = order.clientPhone,
                    metodoPagoId = order.paymentMethodId
                )
                
                try {
                    val response = apiService.registrarPedido(request)
                    if (response.isSuccessful && response.body()?.ok == true) {
                        database.orderDao().markAsSynced(order.id)
                    } else if (!response.isSuccessful) {
                        throw Exception("Error del servidor: ${response.code()}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SyncWorker", "ERROR syncOrders: ${e.message}", e)
                    sendBroadcast("Error al enviar pedidos", true)
                    throw e
                }
            }
        }
    }

    private suspend fun syncRoutes(userId: Long) {
        val prefs = com.cabel.rutacabel.utils.PreferenceManager(applicationContext)
        val employeeId = prefs.getEmpleadoId()
        
        try {
            val response = apiService.getRutasDetalles(employeeId)
            if (response.isSuccessful && response.body()?.success == true) {
                val items = response.body()?.data ?: emptyList()
                if (items.isNotEmpty()) {
                    
                    val distinctRoutes = items.distinctBy { it.rutaId }
                    
                    distinctRoutes.forEach { routeItem ->
                         // UPSERT Route
                         val existingRoute = database.routeDao().getRouteByRemoteId(routeItem.rutaId)
                         
                         if (existingRoute != null) {
                             // Update
                             val updatedRoute = existingRoute.copy(
                                 routeName = routeItem.nombreRuta,
                                 // status = "ACTIVE", // Verify if we should reset status
                                 lastSync = System.currentTimeMillis()
                             )
                             database.routeDao().updateRoute(updatedRoute)
                         } else {
                             // Insert
                             val route = com.cabel.rutacabel.data.local.entities.Route(
                                remoteId = routeItem.rutaId,
                                userId = userId,
                                routeName = routeItem.nombreRuta,
                                startDate = System.currentTimeMillis(),
                                status = "ACTIVE",
                                needsSync = false,
                                lastSync = System.currentTimeMillis()
                            )
                            database.routeDao().insertRoute(route)
                         }
                    }

                    // Save Details (Replace Strategy for Details is usually fine as they are children)
                    // But we must target the correct local route ID if we want to link them properly.
                    // However, entities might not have composite keys or foreign keys set up strictly.
                    // RouteDetail usually links via routeId (remote or local? check entity).
                    
                    // Assuming RouteDetail links via remote RouteId (rutaId in Detail)
                    
                    // To avoid duplicates in details, we should clear details for these specific routes first
                    val routeIds = distinctRoutes.map { it.rutaId }
                    // Implement deleteByRouteId in DAO if missing, or use manual check
                    
                    // For now, let's just insert/replace if collision
                    // But we might accumulate if server removes a visit.
                    // Better verify if we can delete old ones.
                    
                     database.routeDetailDao().deleteAll() // The user wants CLEAN data "limpiar la bd... que no se repita"
                     // This clears ALL details. If we have multiple routes, this clears all. 
                     // Assuming sync returns ALL assigned routes.
                    
                    val details = items.map { item ->
                        com.cabel.rutacabel.data.local.entities.RouteDetail(
                            rutaId = item.rutaId,
                            nombreRuta = item.nombreRuta,
                            consecutivo = item.consecutivo,
                            clienteId = item.clienteId ?: 0,
                            clienteNombre = item.clienteNombre,
                            folioSucursal = item.folioSucursal,
                            latitud = item.latitud,
                            longitud = item.longitud,
                            estatus = item.estatus,
                            visitado = false,
                            lastSync = System.currentTimeMillis()
                        )
                    }
                    
                    if (details.isNotEmpty()) {
                        database.routeDetailDao().insertAll(details)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Don't fail the whole sync for routes, just log
        }
    }

    private suspend fun syncInventoryDownload(userId: Long) {
        val context = applicationContext
        val prefs = context.getSharedPreferences("RutaCABELPrefs", Context.MODE_PRIVATE)
        val branchId = prefs.getInt("branchId", 0)

        if (branchId == 0) return

        sendBroadcast("Sincronizando inventario...", false)

        try {
            // 1. Fetch Prices into memory
            var priceMap: Map<Long, Double> = emptyMap()
            var pendingPrices: Map<Long, List<com.cabel.rutacabel.data.local.entities.ProductPrice>> = emptyMap()
            val now = System.currentTimeMillis()

            val priceResponse = apiService.getPrecios(0)
            android.util.Log.d("SyncWorker", "PRECIOS API: success=${priceResponse.isSuccessful}, code=${priceResponse.code()}")

            if (priceResponse.isSuccessful && priceResponse.body()?.success == true) {
                val prices = priceResponse.body()?.data ?: emptyList()
                android.util.Log.d("SyncWorker", "PRECIOS RECIBIDOS DEL API: ${prices.size}")

                val defaultPrices = mutableMapOf<Long, Double>()
                for (price in prices) {
                    if (!defaultPrices.containsKey(price.productoId) || price.publico == 1 || price.idLista == 1) {
                        defaultPrices[price.productoId] = price.precio
                    }
                }
                priceMap = defaultPrices
                android.util.Log.d("SyncWorker", "PRECIOS DEFAULT MAP: ${priceMap.size} productos con precio")

                pendingPrices = prices.groupBy { it.productoId }.mapValues { (_, productPrices) ->
                    productPrices.map { p ->
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
                }
                android.util.Log.d("SyncWorker", "PENDING PRICES: ${pendingPrices.size} productos, ${pendingPrices.values.flatten().size} precios total")
            } else {
                android.util.Log.e("SyncWorker", "PRECIOS API FALLO: body.success=${priceResponse.body()?.success}, message=${priceResponse.body()?.message}")
            }

            // 2. Fetch Inventory and build batch product list
            val invResponse = apiService.getInventario(branchId)
            if (invResponse.isSuccessful && invResponse.body()?.success == true) {
                val items = invResponse.body()?.data ?: emptyList()
                android.util.Log.d("SyncWorker", "INVENTARIO RECIBIDO: ${items.size} productos")

                // Group items by product ID and sum ONLY POSITIVE quantities
                val groupedItems = items.groupBy { it.productoId }
                
                // Build products in memory, then batch insert
                val productsToUpsert = groupedItems.map { (productId, productItems) ->
                    // Use the first item for metadata, but sum real availability
                    val firstItem = productItems.first()
                    // Sum only positive stocks (ignore negatives like -9)
                    val totalPositiveStock = productItems.filter { it.cantidad > 0 }.sumOf { it.cantidad }
                    
                    val price = priceMap[productId] ?: firstItem.ultimoCosto
                    val existing = database.productDao().getProductByRemoteId(productId)

                    if (existing != null) {
                        existing.copy(
                            code = firstItem.codigo ?: "",
                            name = firstItem.nombre,
                            description = firstItem.presentacion,
                            category = firstItem.almacen,
                            basePrice = price,
                            stock = totalPositiveStock,
                            unit = "PZA",
                            sucursalId = branchId,
                            isActive = true,
                            lastSync = now
                        )
                    } else {
                        com.cabel.rutacabel.data.local.entities.Product(
                            remoteId = productId,
                            code = firstItem.codigo ?: "",
                            name = firstItem.nombre,
                            description = firstItem.presentacion,
                            category = firstItem.almacen,
                            basePrice = price,
                            stock = totalPositiveStock,
                            unit = "PZA",
                            sucursalId = branchId,
                            isActive = true,
                            lastSync = now
                        )
                    }
                }

                // Single batch insert/replace
                if (productsToUpsert.isNotEmpty()) {
                    database.productDao().insertProducts(productsToUpsert)
                    android.util.Log.d("SyncWorker", "PRODUCTOS INSERTADOS: ${productsToUpsert.size}")
                }

                // 3. Batch-save all prices at once right after products
                //    FK CASCADE already wiped old prices when products were re-inserted,
                //    so we only need to insert (no delete needed).
                if (pendingPrices.isNotEmpty()) {
                    val existingRemoteIds = productsToUpsert.mapNotNull { it.remoteId }.toSet()
                    val allPricesToInsert = pendingPrices
                        .filter { it.key in existingRemoteIds }
                        .values.flatten()

                    android.util.Log.d("SyncWorker", "PRECIOS A INSERTAR: ${allPricesToInsert.size} (de ${pendingPrices.values.flatten().size} total, ${existingRemoteIds.size} productos matching)")

                    if (allPricesToInsert.isNotEmpty()) {
                        database.productPriceDao().insertPrices(allPricesToInsert)
                        android.util.Log.d("SyncWorker", "PRECIOS INSERTADOS OK: ${allPricesToInsert.size}")
                    }
                } else {
                    android.util.Log.w("SyncWorker", "PENDING PRICES VACIO - no se guardaron precios")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "ERROR syncInventory: ${e.message}", e)
            e.printStackTrace()
            sendBroadcast("Error al sincronizar inventario", true)
            throw e
        }
    }

    private suspend fun syncCatalogs() {
        try {
            // Categories
            val catResponse = apiService.getCategories()
            if (catResponse.isSuccessful && catResponse.body()?.success == true) {
                catResponse.body()?.data?.let { items ->
                    for (item in items) {
                        upsertCatalog("CATEGORY", item.categoriaId ?: 0, item.nombre)
                    }
                }
            }
            
            // Presentations
            val presResponse = apiService.getPresentations()
            if (presResponse.isSuccessful && presResponse.body()?.success == true) {
                presResponse.body()?.data?.let { items ->
                    for (item in items) {
                        upsertCatalog("PRESENTATION", item.presentacionId ?: 0, item.nombre)
                    }
                }
            }
            
            // Units
            val unitResponse = apiService.getUnits()
            if (unitResponse.isSuccessful && unitResponse.body()?.success == true) {
                unitResponse.body()?.data?.let { items ->
                    for (item in items) {
                        upsertCatalog("UNIT", item.unidadMedidaId ?: 0, item.nombre, item.umSat ?: "")
                    }
                }
            }
            
            // Payment Methods
            val pmResponse = apiService.getPaymentMethods()
            if (pmResponse.isSuccessful && pmResponse.body()?.success == true) {
                pmResponse.body()?.data?.let { items ->
                    for (item in items) {
                        upsertCatalog("PAYMENT_METHOD", item.metodoPagoId ?: 0, item.nombre)
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
             sendBroadcast("Error al sincronizar catálogos", true)
        }
    }

    private suspend fun upsertCatalog(type: String, remoteId: Int, name: String, value: String = "") {
        if (remoteId == 0) return
        val existing = database.catalogDao().getCatalogByRemoteId(type, remoteId)
        if (existing != null) {
            database.catalogDao().updateCatalog(existing.copy(name = name, value = value, lastSync = System.currentTimeMillis()))
        } else {
            database.catalogDao().insertCatalog(
                com.cabel.rutacabel.data.local.entities.Catalog(
                    type = type,
                    remoteId = remoteId,
                    name = name,
                    value = value,
                    lastSync = System.currentTimeMillis()
                )
            )
        }
    }

    private fun sendBroadcast(message: String, isError: Boolean) {
        val intent = android.content.Intent(ACTION_SYNC_STATUS)
        intent.putExtra(EXTRA_MESSAGE, message)
        intent.putExtra(EXTRA_IS_ERROR, isError)
        // Log locally for debugging
        if (isError) {
            android.util.Log.e("SyncWorker", message)
        } else {
            android.util.Log.i("SyncWorker", message)
        }
        applicationContext.sendBroadcast(intent)
    }

    private fun sendSyncLifecycleBroadcast(started: Boolean) {
        val intent = android.content.Intent(ACTION_SYNC_STATUS)
        intent.putExtra(EXTRA_SYNC_STARTED, started)
        intent.putExtra(EXTRA_SYNC_FINISHED, !started)
        if (!started) {
            intent.putExtra(EXTRA_PROGRESS, 100)
        }
        applicationContext.sendBroadcast(intent)
    }

    private fun sendProgressBroadcast(progress: Int, message: String) {
        val intent = android.content.Intent(ACTION_SYNC_STATUS)
        intent.putExtra(EXTRA_PROGRESS, progress)
        intent.putExtra(EXTRA_DETAIL, message)
        intent.putExtra(EXTRA_MESSAGE, message) // Fallback
        applicationContext.sendBroadcast(intent)
        android.util.Log.i("SyncWorker", "[$progress%] $message")
    }

    companion object {
        const val ACTION_SYNC_STATUS = "com.cabel.rutacabel.SYNC_STATUS"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_IS_ERROR = "is_error"
        const val EXTRA_SYNC_STARTED = "sync_started"
        const val EXTRA_SYNC_FINISHED = "sync_finished"
        const val EXTRA_PROGRESS = "sync_progress"
        const val EXTRA_DETAIL = "sync_detail"

        fun scheduleSync(context: Context, userId: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(workDataOf("USER_ID" to userId))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "RutaCabelSync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        }
        
        fun runOnce(context: Context, userId: Long) {
             val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf("USER_ID" to userId))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "RutaCabelSync_OneTime",
                ExistingWorkPolicy.KEEP,
                syncWorkRequest
            )
        }
    }
}
