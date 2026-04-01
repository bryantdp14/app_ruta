package com.cabel.rutacabel.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cabel.rutacabel.data.local.entities.Catalog

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalogs WHERE type = :type ORDER BY name ASC")
    fun getCatalogsByType(type: String): LiveData<List<Catalog>>

    @Query("SELECT * FROM catalogs WHERE type = :type ORDER BY name ASC")
    suspend fun getCatalogsByTypeSync(type: String): List<Catalog>

    @Query("SELECT * FROM catalogs WHERE type = :type AND remoteId = :remoteId LIMIT 1")
    suspend fun getCatalogByRemoteId(type: String, remoteId: Int): Catalog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalog(catalog: Catalog)

    @Update
    suspend fun updateCatalog(catalog: Catalog)

    @Query("DELETE FROM catalogs WHERE type = :type")
    suspend fun deleteCatalogsByType(type: String)
}
