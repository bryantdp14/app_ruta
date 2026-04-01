# Aplicación Móvil de Ventas en Ruta - CABEL

Aplicación Android para gestión de ventas en ruta con diseño moderno en blanco y azul.

## Características Principales

### 1. Inicio de Sesión
- Autenticación segura de usuarios
- Modo offline con base de datos local
- Sincronización automática con servidor remoto

### 2. Tablero de Control
- Inicio y cierre de ruta
- Resumen diario de operaciones
- Métricas en tiempo real (ventas, cobros, clientes visitados)

### 3. Hoja de Ruta
- Lista de clientes con orden de visita
- Búsqueda y filtrado de clientes
- Registro de nuevos clientes
- Acciones rápidas por cliente:
  - Venta
  - Cobro
  - Navegación GPS
  - Registro de incidencias

### 4. Levantamiento de Pedido
- Catálogo de productos con imágenes
- Escáner de código de barras
- Filtros por categoría
- Ajuste de precios para negociación
- Impresión de ticket de venta

### 5. Registro de Cobros
- Múltiples métodos de pago (efectivo, tarjeta, transferencia)
- Registro de referencia de pago
- Impresión de recibo

### 6. Inventario
- Control de productos cargados
- Registro de nuevos productos
- Seguimiento de stock en ruta

### 7. Estadísticas
- Resumen de ventas del día
- Total de cobros realizados
- Tiempo de ruta
- Nuevos clientes registrados

### 8. Incidencias
- Registro con descripción
- Captura fotográfica
- Vinculación con cliente

## Tecnologías Utilizadas

- **Lenguaje**: Kotlin
- **UI**: Material Design 3
- **Base de Datos Local**: Room
- **Sincronización**: Retrofit + WorkManager
- **Navegación**: Navigation Component
- **Arquitectura**: MVVM
- **Coroutines**: Para operaciones asíncronas
- **Impresión**: Bluetooth para impresoras térmicas
- **Escaneo**: ZXing para códigos de barras
- **GPS**: Google Play Services Location

## Configuración

### Requisitos Previos
- Android Studio Arctic Fox o superior
- SDK mínimo: API 24 (Android 7.0)
- SDK objetivo: API 34 (Android 14)

### Configuración del Servidor
Editar `RetrofitClient.kt` y cambiar la URL del servidor:
```kotlin
private const val BASE_URL = "http://TU_SERVIDOR:PUERTO/api/"
```

### Permisos Requeridos
- Internet
- Cámara (para escaneo y fotos de incidencias)
- Ubicación (para GPS)
- Bluetooth (para impresión)

## Estructura del Proyecto

```
app/
├── data/
│   ├── local/
│   │   ├── dao/          # Data Access Objects
│   │   ├── entities/     # Entidades de la base de datos
│   │   └── AppDatabase   # Configuración de Room
│   ├── remote/
│   │   ├── ApiService    # Definición de endpoints
│   │   └── ApiModels     # Modelos de respuesta
│   └── SyncWorker        # Sincronización en segundo plano
├── ui/
│   ├── auth/            # Pantalla de login
│   ├── dashboard/       # Tablero de control
│   ├── route/           # Hoja de ruta
│   ├── inventory/       # Inventario
│   └── statistics/      # Estadísticas
└── utils/
    ├── PreferenceManager # Gestión de sesión
    └── PrinterManager    # Impresión de tickets
```

## Base de Datos

### Sincronización Automática
La aplicación sincroniza automáticamente cada 15 minutos cuando hay conexión a Internet:
- Sube: ventas, cobros, clientes nuevos, incidencias
- Descarga: productos, precios, información de clientes

### Modo Offline
La aplicación funciona completamente sin conexión:
- Todas las operaciones se guardan localmente
- Al recuperar conexión, se sincronizan automáticamente

## Impresión de Tickets

La aplicación soporta impresoras térmicas Bluetooth compatibles con comandos ESC/POS.

### Formato de Ticket de Venta
- Encabezado con logo
- Fecha y hora
- Datos del cliente
- Detalle de productos
- Subtotal, IVA y total
- Método de pago

### Formato de Recibo de Cobro
- Encabezado
- Datos del cliente
- Monto recibido
- Método y referencia de pago

## Compilación

### Debug
```bash
./gradlew assembleDebug
```

### Release
```bash
./gradlew assembleRelease
```

## API Backend (Endpoints Esperados)

### Autenticación
- POST `/auth/login`

### Sincronización
- POST `/sync/clients/upload`
- GET `/sync/clients/download`
- POST `/sync/products/upload`
- GET `/sync/products/download`
- POST `/sync/sales/upload`
- POST `/sync/payments/upload`
- POST `/sync/incidents/upload`
- POST `/sync/routes/upload`

## Colores del Tema

- Azul Principal: #2196F3
- Azul Oscuro: #1976D2
- Azul Claro: #64B5F6
- Azul Acento: #03A9F4
- Blanco: #FFFFFF
- Fondo: #FAFAFA

## Licencia

Propiedad de CABEL - Todos los derechos reservados

## Contacto

Para soporte técnico o consultas, contactar al equipo de desarrollo.
