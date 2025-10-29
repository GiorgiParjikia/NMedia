/*package ru.netology.nmedia.dao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import ru.netology.nmedia.dto.Post

class PostDaoImpl(private val db: SQLiteDatabase) : PostDao {

    private object PostColumns {
        const val TABLE = "Posts"

        const val COLUMN_ID = "id"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_PUBLISHED = "published"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_LIKES = "likes"
        const val COLUMN_LIKED_BY_ME = "likeByMe"
        const val COLUMN_SHARED = "shared"
        const val COLUMN_VIEWS = "views"
        const val COLUMN_VIDEO = "video"

        val ALL = arrayOf(
            COLUMN_ID,
            COLUMN_AUTHOR,
            COLUMN_PUBLISHED,
            COLUMN_CONTENT,
            COLUMN_LIKES,
            COLUMN_LIKED_BY_ME,
            COLUMN_SHARED,
            COLUMN_VIEWS,
            COLUMN_VIDEO
        )
    }

    override fun getAll(): List<Post> {
        val posts = mutableListOf<Post>()
        db.query(
            PostColumns.TABLE,
            PostColumns.ALL,
            null, null, null, null,
            "${PostColumns.COLUMN_ID} DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                posts.add(map(cursor))
            }
        }
        return posts
    }

    override fun save(post: Post): Post {
        val values = ContentValues().apply {
            put(PostColumns.COLUMN_AUTHOR, if (post.author.isBlank()) "Me" else post.author)
            put(PostColumns.COLUMN_CONTENT, post.content)
            put(PostColumns.COLUMN_PUBLISHED, if (post.published.isBlank()) "now" else post.published)
            put(PostColumns.COLUMN_VIDEO, post.video)
            put(PostColumns.COLUMN_LIKES, post.likes)
            put(PostColumns.COLUMN_SHARED, post.shares)
            put(PostColumns.COLUMN_VIEWS, post.views)
            put(PostColumns.COLUMN_LIKED_BY_ME, if (post.likeByMe) 1 else 0)
        }

        val id = if (post.id != 0L) {
            db.update(
                PostColumns.TABLE,
                values,
                "${PostColumns.COLUMN_ID} = ?",
                arrayOf(post.id.toString())
            )
            post.id
        } else {
            db.insert(PostColumns.TABLE, null, values)
        }

        db.query(
            PostColumns.TABLE,
            PostColumns.ALL,
            "${PostColumns.COLUMN_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        ).use { cursor ->
            cursor.moveToNext()
            return map(cursor)
        }
    }

    override fun likeById(id: Long) {
        db.execSQL(
            """
            UPDATE ${PostColumns.TABLE} SET
                ${PostColumns.COLUMN_LIKES} = ${PostColumns.COLUMN_LIKES} + 
                    CASE WHEN ${PostColumns.COLUMN_LIKED_BY_ME} = 1 THEN -1 ELSE 1 END,
                ${PostColumns.COLUMN_LIKED_BY_ME} = 
                    CASE WHEN ${PostColumns.COLUMN_LIKED_BY_ME} = 1 THEN 0 ELSE 1 END
            WHERE ${PostColumns.COLUMN_ID} = ?;
            """.trimIndent(),
            arrayOf(id)
        )
    }

    override fun shareById(id: Long) {
        db.execSQL(
            """
            UPDATE ${PostColumns.TABLE}
            SET ${PostColumns.COLUMN_SHARED} = ${PostColumns.COLUMN_SHARED} + 1
            WHERE ${PostColumns.COLUMN_ID} = ?;
            """.trimIndent(),
            arrayOf(id)
        )
    }

    override fun removeById(id: Long) {
        db.delete(
            PostColumns.TABLE,
            "${PostColumns.COLUMN_ID} = ?",
            arrayOf(id.toString())
        )
    }

    private fun map(c: Cursor): Post = with(c) {
        Post(
            id = getLong(getColumnIndexOrThrow(PostColumns.COLUMN_ID)),
            author = getString(getColumnIndexOrThrow(PostColumns.COLUMN_AUTHOR)),
            published = getString(getColumnIndexOrThrow(PostColumns.COLUMN_PUBLISHED)),
            content = getString(getColumnIndexOrThrow(PostColumns.COLUMN_CONTENT)),
            likes = getLong(getColumnIndexOrThrow(PostColumns.COLUMN_LIKES)),
            shares = getLong(getColumnIndexOrThrow(PostColumns.COLUMN_SHARED)),
            views = getLong(getColumnIndexOrThrow(PostColumns.COLUMN_VIEWS)),
            likeByMe = getInt(getColumnIndexOrThrow(PostColumns.COLUMN_LIKED_BY_ME)) != 0,
            video = getString(getColumnIndexOrThrow(PostColumns.COLUMN_VIDEO))
        )
    }
}

 */