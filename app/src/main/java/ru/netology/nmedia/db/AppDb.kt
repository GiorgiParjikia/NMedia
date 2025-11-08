package ru.netology.nmedia.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun getInstance(context: Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }

        private fun buildDatabase(context: Context): AppDb =
            Room.databaseBuilder(
                context,
                AppDb::class.java,
                "app.db"
            )
                .fallbackToDestructiveMigration() // 🔹 оставляем один раз
                .build()
    }
}