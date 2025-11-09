package ru.netology.nmedia.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {

    // ===== Получение всех постов =====
    @Query("SELECT * FROM Post_Entity ORDER BY id DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) = 0 FROM Post_Entity")
    fun isEmpty(): Flow<Boolean>

    // ===== Вставка / обновление =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    // ===== Обновление контента =====
    @Query("UPDATE Post_Entity SET content = :content WHERE id = :id")
    suspend fun updateById(id: Long, content: String)

    // ===== Удаление =====
    @Query("DELETE FROM Post_Entity WHERE id = :id")
    suspend fun removeById(id: Long)

    // ===== Лайк =====
    @Query(
        """
        UPDATE Post_Entity SET
            likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
            likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
        WHERE id = :id
        """
    )
    suspend fun likeById(id: Long)

    // ===== Получить конкретный пост (для like/remove) =====
    @Query("SELECT * FROM Post_Entity WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: Long): PostEntity?

    // ===== Получить все локальные посты (ещё не отправленные на сервер) =====
    @Query("SELECT * FROM Post_Entity WHERE isLocal = 1")
    suspend fun getUnsynced(): List<PostEntity>

    // ===== Обновить статус (после успешной синхронизации) =====
    @Query("UPDATE Post_Entity SET isLocal = 0 WHERE id = :id")
    suspend fun markSynced(id: Long)
}