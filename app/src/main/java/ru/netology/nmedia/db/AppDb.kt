package ru.netology.nmedia.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.entity.PostEntity

@Database(
    entities = [PostEntity::class],
    version = 2, // 🔹 увеличил версию (было 1)
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {

    abstract fun postDao(): PostDao
}