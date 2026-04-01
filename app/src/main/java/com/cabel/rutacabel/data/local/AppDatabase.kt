package com.cabel.rutacabel.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cabel.rutacabel.data.local.dao.ClientDao
import com.cabel.rutacabel.data.local.dao.IncidentDao
import com.cabel.rutacabel.data.local.dao.InventoryDao
import com.cabel.rutacabel.data.local.dao.OrderDao
import com.cabel.rutacabel.data.local.dao.OrderDetailDao
import com.cabel.rutacabel.data.local.dao.PaymentDao
import com.cabel.rutacabel.data.local.dao.ProductDao
import com.cabel.rutacabel.data.local.dao.ProviderDao
import com.cabel.rutacabel.data.local.dao.RouteDao
import com.cabel.rutacabel.data.local.dao.RouteDetailDao
import com.cabel.rutacabel.data.local.dao.SaleDao
import com.cabel.rutacabel.data.local.dao.SaleDetailDao
import com.cabel.rutacabel.data.local.dao.UserDao
import com.cabel.rutacabel.data.local.entities.Client
import com.cabel.rutacabel.data.local.entities.Incident
import com.cabel.rutacabel.data.local.entities.Inventory
import com.cabel.rutacabel.data.local.entities.Order
import com.cabel.rutacabel.data.local.entities.OrderDetail
import com.cabel.rutacabel.data.local.entities.Payment
import com.cabel.rutacabel.data.local.entities.Product
import com.cabel.rutacabel.data.local.entities.Provider
import com.cabel.rutacabel.data.local.entities.Route
import com.cabel.rutacabel.data.local.entities.RouteDetail
import com.cabel.rutacabel.data.local.entities.Sale
import com.cabel.rutacabel.data.local.entities.SaleDetail
import com.cabel.rutacabel.data.local.entities.User

@Database(
    entities = [
        User::class,
        Client::class,
        Product::class,
        Sale::class,
        SaleDetail::class,
        Payment::class,
        Route::class,
        Incident::class,
        Inventory::class,
        Provider::class,
        RouteDetail::class,
        Order::class,
        OrderDetail::class,
        com.cabel.rutacabel.data.local.entities.Catalog::class,
        com.cabel.rutacabel.data.local.entities.ProductPrice::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun clientDao(): ClientDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun saleDetailDao(): SaleDetailDao
    abstract fun paymentDao(): PaymentDao
    abstract fun routeDao(): RouteDao
    abstract fun incidentDao(): IncidentDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun providerDao(): ProviderDao
    abstract fun routeDetailDao(): RouteDetailDao
    abstract fun orderDao(): OrderDao
    abstract fun orderDetailDao(): OrderDetailDao
    abstract fun catalogDao(): com.cabel.rutacabel.data.local.dao.CatalogDao
    abstract fun productPriceDao(): com.cabel.rutacabel.data.local.dao.ProductPriceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ruta_cabel_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
