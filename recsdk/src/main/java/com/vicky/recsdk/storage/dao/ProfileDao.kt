package com.vicky.recsdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vicky.recsdk.storage.entity.ProfileEntity

@Dao
internal interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ProfileEntity)

    @Query("SELECT * FROM reco_profile WHERE id = 1")
    suspend fun getProfile(): ProfileEntity?

    @Query("DELETE FROM reco_profile")
    suspend fun deleteAll()
}
