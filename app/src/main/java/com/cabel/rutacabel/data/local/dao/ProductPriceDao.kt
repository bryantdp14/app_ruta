package com.cabel.rutacabel.data.local.dao

import androidx.room.*
import com.cabel.rutacabel.data.local.entities.ProductPrice

@Dao
interface ProductPriceDao {
    @Query("SELECT * FROM product_prices WHERE productoId = :productoId")
    suspend fun getPricesByProduct(productoId: Long): List<ProductPrice>

    @Query("SELECT * FROM product_prices WHERE productoId IN (:productIds)")
    suspend fun getPricesByProductIds(productIds: List<Long>): List<ProductPrice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<ProductPrice>)

    @Query("DELETE FROM product_prices WHERE productoId = :productoId")
    suspend fun deletePricesByProduct(productoId: Long)

    @Transaction
    suspend fun replacePrices(productoId: Long, prices: List<ProductPrice>) {
        deletePricesByProduct(productoId)
        insertPrices(prices)
    }
}
