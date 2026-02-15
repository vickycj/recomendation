package com.vicky.recsdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vicky.recsdk.storage.entity.EmbeddingEntity

@Dao
internal interface EmbeddingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: EmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EmbeddingEntity>)

    @Query("SELECT * FROM reco_embeddings WHERE item_id = :itemId")
    suspend fun getEmbedding(itemId: String): EmbeddingEntity?

    @Query("SELECT * FROM reco_embeddings")
    suspend fun getAllEmbeddings(): List<EmbeddingEntity>

    @Query("SELECT * FROM reco_embeddings WHERE item_id IN (:itemIds)")
    suspend fun getEmbeddings(itemIds: List<String>): List<EmbeddingEntity>

    @Query("DELETE FROM reco_embeddings")
    suspend fun deleteAll()
}
