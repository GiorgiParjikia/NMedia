package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PostRemoteKeyEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val key: Long,
    val type: KeyType
) {
    enum class KeyType {
        AFTER,
        BEFORE,
    }
}
