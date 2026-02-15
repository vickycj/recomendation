package com.vicky.recsdk.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vicky.recsdk.storage.dao.CoOccurrenceDao
import com.vicky.recsdk.storage.dao.EmbeddingDao
import com.vicky.recsdk.storage.dao.EventDao
import com.vicky.recsdk.storage.dao.ItemCacheDao
import com.vicky.recsdk.storage.dao.ProfileDao
import com.vicky.recsdk.storage.entity.CoOccurrenceEntity
import com.vicky.recsdk.storage.entity.EmbeddingEntity
import com.vicky.recsdk.storage.entity.EventEntity
import com.vicky.recsdk.storage.entity.ItemCacheEntity
import com.vicky.recsdk.storage.entity.ProfileEntity

@Database(
    entities = [
        EventEntity::class,
        ItemCacheEntity::class,
        ProfileEntity::class,
        CoOccurrenceEntity::class,
        EmbeddingEntity::class
    ],
    version = 2,
    exportSchema = false
)
internal abstract class RecoDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun itemCacheDao(): ItemCacheDao
    abstract fun profileDao(): ProfileDao
    abstract fun coOccurrenceDao(): CoOccurrenceDao
    abstract fun embeddingDao(): EmbeddingDao

    companion object {
        @Volatile
        private var INSTANCE: RecoDatabase? = null

        fun getInstance(context: Context): RecoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecoDatabase::class.java,
                    "reco_sdk_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
