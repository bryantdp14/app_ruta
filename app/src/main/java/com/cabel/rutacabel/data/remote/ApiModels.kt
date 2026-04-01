package com.cabel.rutacabel.data.remote

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: UserResponse?,
    @SerializedName("token") val token: String?
)

data class UserResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String?,
    @SerializedName("roleId") val roleId: Int,
    @SerializedName("role") val role: String,
    @SerializedName("branchId") val branchId: Int?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("photoUrl") val photoUrl: String?
)

data class SyncRequest<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("userId") val userId: Long,
    @SerializedName("lastSync") val lastSync: Long
)

data class SyncResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<T>?,
    @SerializedName("serverTimestamp") val serverTimestamp: Long
)

data class ProviderSyncModel(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("taxId") val taxId: String,
    @SerializedName("address") val address: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("email") val email: String,
    @SerializedName("deliveryDays") val deliveryDays: String,
    @SerializedName("contactName") val contactName: String,
    @SerializedName("username") val username: String,
    @SerializedName("remoteId") val remoteId: Long?
)

data class OrderSyncModel(
    @SerializedName("id") val id: Long,
    @SerializedName("folio") val folio: String,
    @SerializedName("date") val date: String,
    @SerializedName("total") val total: Double,
    @SerializedName("status") val status: String
)

data class PaymentSyncModel(
    @SerializedName("id") val id: Long,
    @SerializedName("clientName") val clientName: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("date") val date: String
)

// Route Details Response
data class RouteDetailsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<RouteDetailItem>?,
    @SerializedName("message") val message: String?
)

data class RouteDetailItem(
    @SerializedName("rutaId") val rutaId: Long,
    @SerializedName("nombreRuta") val nombreRuta: String,
    @SerializedName("consecutivo") val consecutivo: Int,
    @SerializedName("clienteNombre") val clienteNombre: String,
    @SerializedName("ClienteID", alternate = ["ClienteId", "clienteId"]) val clienteId: Long?, // Added
    @SerializedName("folioSucursal") val folioSucursal: String?,
    @SerializedName("latitud") val latitud: Double,
    @SerializedName("longitud") val longitud: Double,
    @SerializedName("estatus") val estatus: Int
)

// Clients Response
data class ClientsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<ClientItem>?,
    @SerializedName("message") val message: String?
)

data class ClientItem(
    @SerializedName("clienteId") val clienteId: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rfc") val rfc: String,
    @SerializedName("direccion") val direccion: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("municipio") val municipio: String,
    @SerializedName("colonia") val colonia: String,
    @SerializedName("codigoPostal") val codigoPostal: Int,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("diasEntrega") val diasEntrega: String,
    @SerializedName("horarioEntrega") val horarioEntrega: String,
    @SerializedName("latitud") val latitud: Double,
    @SerializedName("longitud") val longitud: Double
)

// Client Registration
data class ClienteRegistroRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("direccion") val direccion: String,
    @SerializedName("colonia") val colonia: String,
    @SerializedName("municipio") val municipio: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("codigoPostal") val codigoPostal: Int,
    @SerializedName("sexo") val sexo: Int,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("correoElectronico") val correoElectronico: String,
    @SerializedName("rfc") val rfc: String,
    @SerializedName("regimen") val regimen: String,
    @SerializedName("esProspecto") val esProspecto: Boolean,
    @SerializedName("latitud") val latitud: Double,
    @SerializedName("longitud") val longitud: Double,
    @SerializedName("foto") val foto: String?, // Base64 encoded
    @SerializedName("credito") val credito: Boolean,
    @SerializedName("limiteCredito") val limiteCredito: Double,
    @SerializedName("diasEntrega") val diasEntrega: String,
    @SerializedName("horarioEntrega") val horarioEntrega: String,
    @SerializedName("usuarioId") val usuarioId: Long
)

data class ClienteRegistroResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("clienteId") val clienteId: String?,
    @SerializedName("message") val message: String?
)

// Product Registration
data class ProductoRegistroRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("clave") val clave: String,
    @SerializedName("codigoSAT") val codigoSAT: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("categoriaId") val categoriaId: Int,
    @SerializedName("presentacion") val presentacion: Int,
    @SerializedName("unidadMedida") val unidadMedida: Int,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("stockMinimo") val stockMinimo: Int,
    @SerializedName("stockMaximo") val stockMaximo: Int,
    @SerializedName("cotizacion") val cotizacion: Int,
    @SerializedName("foto") val foto: String?, // Base64 encoded
    @SerializedName("perecedero") val perecedero: Boolean,
    @SerializedName("usuarioId") val usuarioId: Long,
    @SerializedName("sucursalId") val sucursalId: Int
)

data class ProductoRegistroResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("productoId") val productoId: Long?,
    @SerializedName("message") val message: String?
)

// Inventory Response
data class InventarioResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<InventarioItem>?,
    @SerializedName("message") val message: String?
)

data class InventarioItem(
    @SerializedName("ProductoID") val productoId: Long,
    @SerializedName("Codigo") val codigo: String?,
    @SerializedName("Nombre") val nombre: String,
    @SerializedName("Almacen") val almacen: String,
    @SerializedName("Sucursal") val sucursal: String,
    @SerializedName("Cantidad") val cantidad: Int,
    @SerializedName("UltimaCompra") val ultimaCompra: String?,
    @SerializedName("Presentacion") val presentacion: String,
    @SerializedName("UltimoCosto") val ultimoCosto: Double,
    var precioPublico: Double? = null
)

// --- Clients List ---
data class ClientesListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<ClienteItem>?
)

data class ClienteItem(
    @SerializedName("ClienteID") val clienteId: Long,
    @SerializedName("Nombre") val nombre: String,
    @SerializedName("Direccion") val direccion: String,
    @SerializedName("Colonia") val colonia: String?,
    @SerializedName("Municipio") val municipio: String?,
    @SerializedName("Estado") val estado: String?,
    @SerializedName("Telefono") val telefono: String?,
    @SerializedName("CorreoElectronico") val email: String?,
    @SerializedName("RFC") val rfc: String?,
    @SerializedName("Regimen") val regimen: String?,
    @SerializedName("EsProspecto") val esProspecto: Int,
    @SerializedName("Credito") val credito: Int,
    @SerializedName("LimiteCredito") val limiteCredito: Double,
    @SerializedName("Latitud") val latitude: Double?,
    @SerializedName("Longitud") val longitude: Double?,
    @SerializedName("UltimaVisita") val ultimaVisita: String?,
    @SerializedName("Adeudo") val adeudo: Int? = 0,
    @SerializedName("MontoAdeudo") val montoAdeudo: Double? = 0.0
)

// --- Product Prices ---
data class PreciosResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<PrecioItem>?
)

data class PrecioItem(
    @SerializedName("ProductoID") val productoId: Long,
    @SerializedName("Producto") val producto: String?,
    @SerializedName("Categoria") val categoria: String?,
    @SerializedName("idLista") val idLista: Int,
    @SerializedName("ListaPrecio") val listaPrecio: String,
    @SerializedName("PrecioID") val precioId: Long,
    @SerializedName("Precio") val precio: Double,
    @SerializedName("CantMin") val cantMin: Int?,
    @SerializedName("CantMax") val cantMax: Int?,
    @SerializedName("Publico") val publico: Int?,
    @SerializedName("FechaMod") val fechaMod: String?
)

// --- Cobros ---
data class CobrosListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<CuentaPorCobrarItem>?
)

data class CuentaPorCobrarItem(
    @SerializedName("FolioSucursal") val folioSucursal: String,
    @SerializedName("Monto") val monto: Double,
    @SerializedName("FechaVencimiento") val fechaVencimiento: String?,
    @SerializedName("MetodoPago") val metodoPago: String?,
    @SerializedName("Referencia") val referencia: String?,
    @SerializedName("Estado") val estado: Int,
    @SerializedName("ClienteNombre") val clienteNombre: String,
    @SerializedName("FechaVenta") val fechaVenta: String?,
    @SerializedName("OrigenVenta") val origenVenta: Int,
    @SerializedName("Sucursal") val sucursal: String?
)

data class CobroRegistroRequest(
    @SerializedName("Folio") val folio: String,
    @SerializedName("Monto") val monto: Double,
    @SerializedName("MetodoPagoID") val metodoPagoId: Int,
    @SerializedName("Ref") val referencia: String,
    @SerializedName("Comentarios") val comentarios: String?,
    @SerializedName("SucID") val sucursalId: Int,
    @SerializedName("UsuID") val usuarioId: Long
)

data class CobroRegistroResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("cobroId") val cobroId: String?
)


// --- Orders ---
data class OrderRegisterRequest(
    @SerializedName("productos") val productos: List<OrderItemRequest>,
    @SerializedName("sucursal") val sucursal: Int,
    @SerializedName("total") val total: Double,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tel") val tel: String,
    @SerializedName("metodoPagoId") val metodoPagoId: Int
)

data class OrderItemRequest(
    @SerializedName("producto") val producto: Long,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("precio") val precio: Double
)

data class OrderResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("msg") val msg: String,
    @SerializedName("data") val data: String? // Folio or URL
)

// --- Catalogs ---
data class CatalogItem(
    @SerializedName("CategoriaID", alternate = ["CategoriaId", "categoriaId"]) val categoriaId: Int?,
    @SerializedName("MetodoPagoID", alternate = ["MetodoPagoId", "metodoPagoId"]) val metodoPagoId: Int?,
    @SerializedName("PresentacionID", alternate = ["PresentacionId", "presentacionId"]) val presentacionId: Int?,
    @SerializedName("UnidadMedidaID", alternate = ["UnidadMedidaId", "unidadMedidaId"]) val unidadMedidaId: Int?,
    @SerializedName("UMSAT") val umSat: String?,
    @SerializedName("Nombre", alternate = ["nombre"]) val nombre: String
)

data class CatalogResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<CatalogItem>?,
    @SerializedName("message") val message: String?
)
