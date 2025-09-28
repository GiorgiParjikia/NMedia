package ru.netology.nmedia.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostDaoImpl

class AppDb private constructor(db: SQLiteDatabase) {
    val postDao = PostDaoImpl(db)

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun getInstance(context: Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: AppDb(
                    buildDatabase(context, arrayOf(PostDao.DDL))  // <-- тут PostDao.DDL
                ).also { instance = it }
            }

        private fun buildDatabase(context: Context, ddls: Array<String>) =
            DbHelper(
                context = context,
                dbVersion = 1,
                dbName = "app.db",
                DDLs = ddls
            ).writableDatabase
    }
}