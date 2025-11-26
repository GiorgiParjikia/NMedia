package ru.netology.nmedia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity.KeyType

@Dao
interface PostRemoteKeyDao {

    @Query("SELECT id FROM PostRemoteKeyEntity WHERE type = :type")
    suspend fun getKey(type: KeyType): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PostRemoteKeyEntity)

    @Query("DELETE FROM PostRemoteKeyEntity WHERE type = :type")
    suspend fun delete(type: KeyType)
}
