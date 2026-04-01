# Configuración de Impresora GOOJPRT PT210 (MTP-II)

## Especificaciones de la Impresora

**Modelo:** GOOJPRT PT210 / MTP-II  
**Tipo:** Impresora térmica portátil Bluetooth  
**Ancho de papel:** 58mm  
**Tecnología:** ESC/POS compatible  
**Conectividad:** Bluetooth 4.0  

---

## Configuración en la Aplicación Ruta CABEL

### 1. Emparejamiento Bluetooth

**Pasos para vincular la impresora:**

1. **Encender la impresora** PT210 presionando el botón de encendido
2. El LED azul parpadeará indicando que está en modo emparejamiento
3. En tu dispositivo Android:
   - Ir a **Configuración > Bluetooth**
   - Buscar dispositivos disponibles
   - Seleccionar **"PT210"**, **"MTP-II"** o **"GOOJPRT"**
   - PIN por defecto: **1234** o **0000**
4. Una vez emparejado, el LED quedará fijo en azul

### 2. Conectar desde la App

```kotlin
val printerManager = PrinterManager(context)
val pairedPrinters = printerManager.getPairedPrinters()

val pt210 = pairedPrinters.find { 
    it.name?.contains("PT210", ignoreCase = true) == true ||
    it.name?.contains("MTP", ignoreCase = true) == true
}

if (pt210 != null) {
    val connected = printerManager.connectToPrinter(pt210)
    if (connected) {
    }
}
```

### 3. Imprimir Ticket de Venta

```kotlin
val items = listOf(
    TicketItem("Coca-Cola 600ml", 5, 15.0, 75.0),
    TicketItem("Sabritas Originales", 3, 12.50, 37.50)
)

printerManager.printSaleTicket(
    clientName = "Tienda La Esquina",
    items = items,
    subtotal = 112.50,
    tax = 18.0,
    total = 130.50,
    paymentMethod = "Efectivo"
)
```

### 4. Imprimir Recibo de Cobro

```kotlin
printerManager.printPaymentTicket(
    clientName = "Abarrotes Juárez",
    amount = 850.00,
    paymentMethod = "Transferencia",
    reference = "REF-202402-001"
)
```

---

## Características Implementadas para PT210

### ✅ Comandos ESC/POS Optimizados

1. **Inicialización mejorada**
   - Delay de 50ms después de INIT para estabilidad
   - Reset completo de configuración

2. **Tamaños de fuente**
   - Normal (0x00) - texto estándar
   - Doble (0x11) - altura y ancho x2
   - Grande (0x22) - altura y ancho x3

3. **Formato de texto**
   - Negritas (BOLD_ON/OFF)
   - Alineación (izquierda, centro, derecha)
   - Subrayado (UNDERLINE_ON/OFF)

4. **Control de papel**
   - Corte automático al final
   - Feed de 3 líneas antes de cortar
   - Optimizado para papel de 58mm

### ✅ Ancho de Impresión (58mm)

- Máximo **32 caracteres** por línea en fuente normal
- **16 caracteres** en fuente doble
- Tickets ajustados a este ancho

### ✅ Formato de Tickets

**Ticket de Venta:**
```
        RUTA CABEL        (grande, centrado)
   Sistema de Ventas      (normal, centrado)
--------------------------------
Fecha: 13/02/2026 15:30
Cliente: Tienda La Esquina
================================
Producto          Cant  Total   (negrita)
--------------------------------
Coca-Cola 600ml      5 $75.00
Sabritas Original    3 $37.50
================================
Subtotal:            $112.50
IVA (16%):            $18.00
--------------------------------
TOTAL:               $130.50    (doble, negrita)
================================
Metodo de pago: Efectivo

    Gracias por su compra!
       Vuelva pronto
```

**Recibo de Cobro:**
```
        RUTA CABEL        (grande, centrado)
      Recibo de Cobro      
--------------------------------
Fecha: 13/02/2026 15:45
Cliente: Abarrotes Juárez
================================
     MONTO COBRADO         (grande)
       $850.00             (doble, negrita)
================================
Metodo: Transferencia
Referencia: REF-202402-001

   Gracias por su pago!
   Conserve este recibo
```

---

## Solución de Problemas

### ⚠️ No se detecta la impresora

**Solución:**
1. Verificar que la impresora esté encendida (LED azul)
2. Desemparejar y volver a emparejar en configuración Bluetooth
3. Reiniciar la aplicación
4. Asegurarse de que los permisos Bluetooth estén concedidos

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

### ⚠️ Imprime caracteres extraños

**Solución:**
- El código usa **UTF-8** con compatibilidad para caracteres especiales españoles
- Si hay problemas, cambiar a **Windows-1252**:

```kotlin
write(text.toByteArray(Charsets.ISO_8859_1))
```

### ⚠️ No corta el papel

**Solución:**
1. Verificar que la impresora tenga papel instalado
2. Algunos modelos requieren comando diferente:

```kotlin
private val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x00)
```

### ⚠️ Se desconecta frecuentemente

**Solución:**
1. Mantener el dispositivo cerca de la impresora (máx 10 metros)
2. Cargar completamente la batería de la PT210
3. Agregar reconexión automática:

```kotlin
fun reconnect(device: BluetoothDevice): Boolean {
    disconnect()
    Thread.sleep(1000)
    return connectToPrinter(device)
}
```

---

## Especificaciones Técnicas PT210

| Característica | Valor |
|----------------|-------|
| Ancho de papel | 58mm |
| Velocidad | 70mm/seg |
| Resolución | 203 DPI (8 dots/mm) |
| Batería | 1500mAh Li-ion |
| Autonomía | ~5 horas / 150 tickets |
| Bluetooth | BT 4.0 |
| Distancia | Hasta 10 metros |
| Comandos | ESC/POS compatible |

---

## Mantenimiento

### Limpieza del Cabezal Térmico

**Frecuencia:** Cada 1000 tickets o cuando la impresión se vea difusa

**Procedimiento:**
1. Apagar la impresora
2. Abrir tapa de papel
3. Limpiar el cabezal con alcohol isopropílico y un paño suave
4. Dejar secar 5 minutos
5. Cerrar tapa e imprimir ticket de prueba

### Cambio de Rollo de Papel

**Papel recomendado:** Térmico 58mm x 30-50mm diámetro

**Instalación:**
1. Abrir tapa
2. Insertar rollo con papel saliendo por arriba
3. Dejar sobresalir 2-3cm
4. Cerrar tapa firmemente
5. Imprimir ticket de prueba

### Carga de Batería

- **Tiempo de carga:** 2-3 horas
- **Indicador:** LED rojo durante carga, verde al completar
- **Cable:** Micro USB 5V/1A
- **Recomendación:** No usar mientras carga para mayor durabilidad

---

## Integración Completa en la App

Para usar en cualquier parte de la aplicación:

```kotlin
class SaleActivity : AppCompatActivity() {
    private lateinit var printerManager: PrinterManager
    private var connectedPrinter: BluetoothDevice? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printerManager = PrinterManager(this)
        setupPrinter()
    }
    
    private fun setupPrinter() {
        val printers = printerManager.getPairedPrinters()
        connectedPrinter = printers.firstOrNull()
        
        connectedPrinter?.let {
            printerManager.connectToPrinter(it)
        }
    }
    
    private fun completeSale() {
        val items = listOf()
        
        val success = printerManager.printSaleTicket(
            clientName = selectedClient.name,
            items = items,
            subtotal = calculateSubtotal(),
            tax = calculateTax(),
            total = calculateTotal(),
            paymentMethod = paymentMethod
        )
        
        if (success) {
            Toast.makeText(this, "Ticket impreso", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error al imprimir", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        printerManager.disconnect()
    }
}
```

---

## Notas Importantes

1. **Batería:** La PT210 tiene batería integrada, perfecta para uso en ruta
2. **Portabilidad:** Diseño compacto (110 x 80 x 45mm), cabe en bolsillo
3. **Durabilidad:** Resistente a golpes y uso rudo en campo
4. **Compatibilidad:** Funciona con comandos estándar ESC/POS
5. **Rendimiento:** Imprime rápido (70mm/seg), ideal para alta demanda

---

## Soporte

Para problemas técnicos con la impresora GOOJPRT PT210:
- Manual oficial: https://www.goojprt.com
- Firmware actualizado disponible en sitio del fabricante
- Soporte técnico de la app: Contactar equipo de desarrollo CABEL
