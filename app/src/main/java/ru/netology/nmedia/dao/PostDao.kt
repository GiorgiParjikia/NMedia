package ru.netology.nmedia.dao

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {

    // ===== Все видимые посты =====
    @Query("SELECT * FROM Post_Entity WHERE isHidden = 0 ORDER BY id DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) = 0 FROM Post_Entity WHERE isHidden = 0")
    fun isEmpty(): Flow<Boolean>

    // ===== Последний ID =====
    @Query("SELECT MAX(id) FROM Post_Entity")
    suspend fun getLatestId(): Long?

    // ===== Insert =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    // ===== Update =====
    @Query("UPDATE Post_Entity SET content = :content WHERE id = :id")
    suspend fun updateById(id: Long, content: String)

    // ===== Delete =====
    @Query("DELETE FROM Post_Entity WHERE id = :id")
    suspend fun removeById(id: Long)

    // ===== Лайк =====
    @Query("""
        UPDATE Post_Entity SET
            likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
            likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
        WHERE id = :id
    """)
    suspend fun likeById(id: Long)

    // ===== Получить пост =====
    @Query("SELECT * FROM Post_Entity WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: Long): PostEntity?

    // ===== Локальные посты =====
    @Query("SELECT * FROM Post_Entity WHERE isLocal = 1")
    suspend fun getUnsynced(): List<PostEntity>

    @Query("UPDATE Post_Entity SET isLocal = 0 WHERE id = :id")
    suspend fun markSynced(id: Long)

    // ===== Hidden =====
    @Query("SELECT COUNT(*) FROM Post_Entity WHERE isHidden = 1")
    suspend fun countHidden(): Int

    @Query("UPDATE Post_Entity SET isHidden = 0 WHERE isHidden = 1")
    suspend fun revealHiddenPosts()

    // ===== ЕДИНСТВЕННЫЙ PagingSource =====
    @Query("SELECT * FROM Post_Entity ORDER BY id DESC")
    fun pagingSource(): PagingSource<Int, PostEntity>

    // ===== Очистка =====
    @Query("DELETE FROM Post_Entity")
    suspend fun clear()
}