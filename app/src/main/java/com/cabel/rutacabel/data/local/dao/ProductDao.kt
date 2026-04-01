package com.cabel.rutacabel.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cabel.rutacabel.data.local.entities.Product

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllProducts(): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Long): Product?

    @Query("SELECT * FROM products WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getProductByRemoteId(remoteId: Long): Product?

    @Query("SELECT * FROM products WHERE code = :code OR barcode = :code")
    suspend fun getProductByCode(code: String): Product?

    @Query("SELECT * FROM products WHERE (name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%') AND isActive = 1 ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun searchProductsPaged(query: String, limit: Int, offset: Int): List<Product>

    @Query("SELECT * FROM products WHERE (name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%') AND isActive = 1")
    suspend fun searchProducts(query: String): List<Product>

    @Query("SELECT * FROM products WHERE category = :category AND isActive = 1")
    suspend fun getProductsByCategory(category: String): List<Product>

    @Query("SELECT DISTINCT category FROM products WHERE isActive = 1 ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT * FROM products WHERE needsSync = 1")
    suspend fun getProductsToSync(): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET needsSync = 1 WHERE id = :productId")
    suspend fun markForSync(productId: Long)

    @Query("SELECT COUNT(*) FROM products WHERE needsSync = 1")
    suspend fun getPendingSyncCount(): Int

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    suspend fun getTotalCount(): Int
}
