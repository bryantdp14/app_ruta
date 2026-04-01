package com.cabel.rutacabel.data.remote

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("api/sync/clients/upload")
    suspend fun syncClientsUpload(@Body request: SyncRequest<out Any>): Response<SyncResponse<Any>>
    
    @GET("api/sync/clients/download")
    suspend fun syncClientsDownload(
        @Query("userId") userId: Long,
        @Query("lastSync") lastSync: Long
    ): Response<SyncResponse<Any>>
    
    @POST("api/sync/products/upload")
    suspend fun syncProductsUpload(@Body request: SyncRequest<out Any>): Response<SyncResponse<Any>>
    
    @GET("api/sync/products/download")
    suspend fun syncProductsDownload(
        @Query("userId") userId: Long,
        @Query("lastSync") lastSync: Long
    ): Response<SyncResponse<Any>>

    @POST("api/sync/providers/upload")
    suspend fun syncProvidersUpload(@Body request: SyncRequest<out Any>): Response<SyncResponse<Any>>

    @GET("api/sync/proveedores/download")
    suspend fun syncProvidersDownload(@Query("provID") provID: Int): Response<SyncResponse<Any>>

    // --- Sales & Orders ---
    @POST("api/sales/direct")
    suspend fun createDirectSale(@Body request: Any): Response<SyncResponse<Any>>

    @POST("api/sales/convert")
    suspend fun convertSale(@Body request: Any): Response<SyncResponse<Any>>

    @GET("api/orders/online")
    suspend fun getOnlineOrders(@Query("sucId") sucId: Int): Response<SyncResponse<Any>>

    @GET("api/orders/{folio}/details")
    suspend fun getOrderDetails(
        @Path("folio") folio: Int,
        @Query("sucId") sucId: Int
    ): Response<SyncResponse<Any>>

    @POST("api/orders/register")
    suspend fun registrarPedido(@Body request: OrderRegisterRequest): Response<OrderResponse>

    @GET("api/cobros/list")
    suspend fun getCobrosPendientes(
        @Query("estCode") estCode: Int,
        @Query("sucId") sucId: Int
    ): Response<CobrosListResponse>

    @POST("api/cobros/register")
    suspend fun registrarCobro(@Body request: CobroRegistroRequest): Response<CobroRegistroResponse>
    
    // --- Routes ---
    @GET("api/routes/details")
    suspend fun getRutasDetalles(
        @Query("empleadoId") empleadoId: Long
    ): Response<RouteDetailsResponse>
    
    @GET("api/clients/available-for-route")
    suspend fun getClientesParaRuta(): Response<ClientsResponse>
    
    // --- Client Registration ---
    @POST("api/clients/register")
    suspend fun registrarCliente(@Body request: ClienteRegistroRequest): Response<ClienteRegistroResponse>
    
    // --- Product Registration ---
    @POST("api/products/register")
    suspend fun registrarProducto(@Body request: ProductoRegistroRequest): Response<ProductoRegistroResponse>
    
    // --- Inventory ---
    @GET("api/inventory/list")
    suspend fun getInventario(
        @Query("sucursalId") sucursalId: Int
    ): Response<InventarioResponse>
    
    // --- Clients List ---
    @GET("api/clients/list")
    suspend fun getClientes(
        @Query("sucursalId") sucursalId: Int
    ): Response<ClientesListResponse>
    
    // --- Product Prices ---
    @GET("api/precios/{productId}")
    suspend fun getPrecios(
        @Path("productId") productId: Int = 0
    ): Response<PreciosResponse>

    // --- Catalogs ---
    @GET("api/catalogs/categories")
    suspend fun getCategories(@Query("catId") catId: Int = 0): Response<CatalogResponse>

    @GET("api/catalogs/units")
    suspend fun getUnits(@Query("umId") umId: Int = 0): Response<CatalogResponse>

    @GET("api/catalogs/payment-methods")
    suspend fun getPaymentMethods(): Response<CatalogResponse>

    @GET("api/catalogs/presentations")
    suspend fun getPresentations(@Query("estId") estId: Int = 0): Response<CatalogResponse>
}
